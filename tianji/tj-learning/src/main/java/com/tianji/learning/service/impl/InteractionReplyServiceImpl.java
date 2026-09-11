package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.remark.RemarkClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domian.dto.ReplyFormDTO;
import com.tianji.learning.domian.po.InteractionQuestion;
import com.tianji.learning.domian.po.InteractionReply;
import com.tianji.learning.domian.query.ReplyPageQuery;
import com.tianji.learning.domian.vo.ReplyVO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.enums.QuestionStatus;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.mq.message.PointsRecordMessage;
import com.tianji.learning.service.IInteractionReplyService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动问题的回答或评论 服务实现类
 * </p>
 *
 * @author wyy
 */
@Service
@RequiredArgsConstructor
public class InteractionReplyServiceImpl extends ServiceImpl<InteractionReplyMapper, InteractionReply> implements IInteractionReplyService {

    private final InteractionQuestionMapper questionMapper;
    private final UserClient userClient;
    private final RemarkClient remarkClient;

    private final RabbitMqHelper mqHelper;
    /**
     *
     *保存评论
     */
    @Override
    @Transactional
    public void saveReply(ReplyFormDTO replyDTO) {
        Long userId = UserContext.getUser();

        // 1. 校验问题是否存在
        InteractionQuestion question = questionMapper.selectById(replyDTO.getQuestionId());
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }

        // 2. 如果是评论，校验回答是否存在
        if (replyDTO.getAnswerId() != null) {
            InteractionReply answer = getById(replyDTO.getAnswerId());
            if (answer == null) {
                throw new BadRequestException("回答不存在");
            }
        }

        // 3. 保存回复
        InteractionReply reply = BeanUtils.copyBean(replyDTO, InteractionReply.class);
        reply.setUserId(userId);
        reply.setCreateTime(LocalDateTime.now());
        reply.setUpdateTime(LocalDateTime.now());
        save(reply);

        // 4. 如果是直接回答，更新问题的最新回答id和回答数
        if (replyDTO.getAnswerId() == null) {
            question.setLatestAnswerId(reply.getId());
            question.setAnswerTimes(question.getAnswerTimes() + 1);
            questionMapper.updateById(question);

            // 5. 发送积分 MQ
            PointsRecordMessage message = PointsRecordMessage.of(
                    userId,
                    PointsRecordType.QA.getMaxPoints(),
                    PointsRecordType.QA.getValue());
            mqHelper.send(
                    MqConstants.Exchange.LEARNING_EXCHANGE,
                    MqConstants.Key.WRITE_REPLY,
                    message);
        }

        // 6. 如果是学员提交，问题状态改为未查看
        if (Boolean.TRUE.equals(replyDTO.getIsStudent())) {
            question.setStatus(QuestionStatus.UN_CHECK);
            questionMapper.updateById(question);
        }
    }

    /**
     * 分页查询回答或评论列表
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        // 1. 参数校验
        if (query.getQuestionId() == null && query.getAnswerId() == null) {
            throw new BadRequestException("问题id和回答id不能同时为空");
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<InteractionReply> wrapper = new LambdaQueryWrapper<>();

        if (query.getAnswerId() == null) {
            wrapper.eq(InteractionReply::getQuestionId, query.getQuestionId())
                    .isNull(InteractionReply::getAnswerId);
        } else {
            wrapper.eq(InteractionReply::getAnswerId, query.getAnswerId());
        }

        wrapper.eq(InteractionReply::getHidden, false);
        wrapper.orderByDesc(InteractionReply::getCreateTime);

        // 3. 分页查询
        Page<InteractionReply> page = page(query.toMpPage(), wrapper);
        List<InteractionReply> records = page.getRecords();

        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        // 4. 查询用户信息
        Set<Long> userIds = records.stream()
                .filter(r -> !r.getAnonymity())
                .map(InteractionReply::getUserId)
                .collect(Collectors.toSet());

        Map<Long, UserDTO> userMap = new HashMap<>();
        if (CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 5. 查询点赞状态（新增）
        Set<Long> likedIds = new HashSet<>();
        if (CollUtils.isNotEmpty(records)) {
            List<Long> replyIds = records.stream()
                    .map(InteractionReply::getId)
                    .collect(Collectors.toList());
            // 调用评价服务查询当前用户已点赞的ID
            likedIds = remarkClient.getLikedStatus(replyIds, "reply");
            if (likedIds == null) {
                likedIds = new HashSet<>();
            }
        }

        // 6. 统计评论数量
        Map<Long, Integer> replyCountMap = new HashMap<>();
        if (query.getAnswerId() == null) {
            List<Long> replyIds = records.stream()
                    .map(InteractionReply::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<InteractionReply> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.in(InteractionReply::getAnswerId, replyIds)
                    .eq(InteractionReply::getHidden, false);
            List<InteractionReply> childReplies = list(countWrapper);

            replyCountMap = childReplies.stream()
                    .collect(Collectors.groupingBy(
                            InteractionReply::getAnswerId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    ));
        }
        // 7. 提取为 final 变量（在 lambda 外部）
        final Map<Long, UserDTO> finalUserMap = userMap;
        final Map<Long, Integer> finalReplyCountMap = replyCountMap;
        final Set<Long> finalLikedIds = likedIds;
        final Long answerId = query.getAnswerId();

        // 7. 封装VO
        List<ReplyVO> voList = records.stream().map(reply -> {
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);

            // 7.1 用户信息（匿名则不返回）
            if (!reply.getAnonymity()) {
                UserDTO user = finalUserMap.get(reply.getUserId());
                if (user != null) {
                    vo.setUserName(user.getName());
                    vo.setUserIcon(user.getIcon());
                }
            }

            // 7.2 评论数量（仅回答）
            if (query.getAnswerId() == null) {
                vo.setReplyTimes(finalReplyCountMap.getOrDefault(reply.getId(), 0));
            }

            // 7.3 目标用户昵称（仅评论）
            if (query.getAnswerId() != null && reply.getTargetUserId() != null) {
                UserDTO targetUser = userClient.queryUserById(reply.getTargetUserId());
                if (targetUser != null) {
                    vo.setTargetUserName(targetUser.getName());
                }
            }

            // ========== 7.4 新增：当前用户是否已点赞 ==========
            vo.setLiked(finalLikedIds.contains(reply.getId()));

            return vo;
        }).collect(Collectors.toList());

        return PageDTO.of(page, voList);
    }


    /**
     * 管理端分页查询回答或评论列表
     * @param query 查询条件
     * @return 分页结果
     */
    @Override
    public PageDTO<ReplyVO> queryReplyPageForAdmin(ReplyPageQuery query) {
        // 1. 参数校验
        if (query.getQuestionId() == null && query.getAnswerId() == null) {
            throw new BadRequestException("问题id和回答id不能同时为空");
        }

        // 2. 构建查询条件
        LambdaQueryWrapper<InteractionReply> wrapper = new LambdaQueryWrapper<>();

        if (query.getAnswerId() == null) {
            wrapper.eq(InteractionReply::getQuestionId, query.getQuestionId())
                    .isNull(InteractionReply::getAnswerId);
        } else {
            wrapper.eq(InteractionReply::getAnswerId, query.getAnswerId());
        }

        // 管理端：不隐藏，查询所有（包括 hidden=true）
        wrapper.orderByDesc(InteractionReply::getCreateTime);

        // 3. 分页查询
        Page<InteractionReply> page = page(query.toMpPage(), wrapper);
        List<InteractionReply> records = page.getRecords();

        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        // 4. 查询用户信息（管理端无视匿名，全部查询；同时查目标用户）
        Set<Long> userIds = new HashSet<>();
        for (InteractionReply reply : records) {
            userIds.add(reply.getUserId());
            if (reply.getTargetUserId() != null) {
                userIds.add(reply.getTargetUserId());
            }
        }

        Map<Long, UserDTO> userMap = new HashMap<>();
        if (CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap.putAll(users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u)));
        }

        // 5. 统计评论数量（管理端包括被隐藏的）
        Map<Long, Integer> replyCountMap = new HashMap<>();
        if (query.getAnswerId() == null) {
            List<Long> replyIds = records.stream()
                    .map(InteractionReply::getId)
                    .collect(Collectors.toList());

            LambdaQueryWrapper<InteractionReply> countWrapper = new LambdaQueryWrapper<>();
            countWrapper.in(InteractionReply::getAnswerId, replyIds);
            List<InteractionReply> childReplies = list(countWrapper);

            replyCountMap.putAll(childReplies.stream()
                    .collect(Collectors.groupingBy(
                            InteractionReply::getAnswerId,
                            Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                    )));
        }

        // 6. 封装VO
        List<ReplyVO> voList = records.stream().map(reply -> {
            ReplyVO vo = BeanUtils.copyBean(reply, ReplyVO.class);

            // 管理端：无视匿名，全部返回用户信息
            UserDTO user = userMap.get(reply.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
                vo.setUserIcon(user.getIcon());
            }

            if (query.getAnswerId() == null) {
                vo.setReplyTimes(replyCountMap.getOrDefault(reply.getId(), 0));
            }

            if (query.getAnswerId() != null && reply.getTargetUserId() != null) {
                UserDTO targetUser = userMap.get(reply.getTargetUserId());
                if (targetUser != null) {
                    vo.setTargetUserName(targetUser.getName());
                }
            }

            return vo;
        }).collect(Collectors.toList());

        return PageDTO.of(page, voList);
    }



    /**
     * 管理端隐藏或显示回答/评论
     * @param id 回答/评论id
     * @param hidden 是否隐藏
     */
    @Override
    @Transactional
    public void hiddenReply(Long id, Boolean hidden) {
        // 1. 查询回复是否存在
        InteractionReply reply = getById(id);
        if (reply == null) {
            throw new BadRequestException("回复不存在");
        }

        // 2. 更新隐藏状态
        reply.setHidden(hidden);
        updateById(reply);

        // 3. 如果是回答，且要隐藏，则其下的所有评论也要隐藏
        if (reply.getAnswerId() == null && Boolean.TRUE.equals(hidden)) {
            LambdaQueryWrapper<InteractionReply> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(InteractionReply::getAnswerId, id);
            List<InteractionReply> childReplies = list(wrapper);

            if (CollUtils.isNotEmpty(childReplies)) {
                childReplies.forEach(r -> r.setHidden(true));
                updateBatchById(childReplies);
            }
        }
    }


}
