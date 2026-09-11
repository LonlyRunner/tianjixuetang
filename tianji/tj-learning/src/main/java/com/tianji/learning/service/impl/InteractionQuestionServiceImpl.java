package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.cache.CategoryCache;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.search.SearchClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domian.dto.QuestionFormDTO;
import com.tianji.learning.domian.po.InteractionQuestion;
import com.tianji.learning.domian.po.InteractionReply;
import com.tianji.learning.domian.query.QuestionAdminPageQuery;
import com.tianji.learning.domian.query.QuestionPageQuery;
import com.tianji.learning.domian.vo.QuestionAdminVO;
import com.tianji.learning.domian.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;  // 如果还没有的话

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author wyy
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {


    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;
    private final SearchClient searchClient;
    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;
    private final CategoryCache categoryCache;


    /**
     * 保存问题
     * @param questionDTO 问题表单信息
     */
    @Override
    @Transactional
    public void saveQuestion(QuestionFormDTO questionDTO) {
        // 1.获取登录用户
        Long userId = UserContext.getUser();
        // 2.数据转换
        InteractionQuestion question = BeanUtils.toBean(questionDTO, InteractionQuestion.class);
        // 3.补充数据
        question.setUserId(userId);
        // 4.保存问题
        save(question);
    }


    /**
     * 分页查询问题
     * @param query 查询条件
     * @return 问题列表
     */

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id和小节id不能都为空
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 2.分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()) { // 只查询非匿名的问题
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 3.2.根据id查询最近一次回答
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){ // 匿名用户不做查询
                    userIds.add(reply.getUserId());
                }
            }
        }

        // 3.3.根据id查询用户信息（提问者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            // 4.1.将PO转为VO
            QuestionVO vo = BeanUtils.copyBean(r, QuestionVO.class);
            vo.setUserId(null);
            voList.add(vo);
            // 4.2.封装提问者信息
            if(!r.getAnonymity()){
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }

            // 4.3.封装最近一次回答的信息
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if(!reply.getAnonymity()){// 匿名用户直接忽略
                    UserDTO user = userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(user.getName());
                }

            }
        }

        return PageDTO.of(page, voList);
    }

    /**
     * 根据id查询问题
     * @param id 问题id
     * @return 问题信息
     */
    @Override
    public QuestionVO queryQuestionById(Long id) {
        // 1.根据id查询数据
        InteractionQuestion question = getById(id);
        // 2.数据校验
        if(question == null || question.getHidden()){
            // 没有数据或者是被隐藏了
            return null;
        }
        // 3.查询提问者信息
        UserDTO user = null;
        if(!question.getAnonymity()){
            user = userClient.queryUserById(question.getUserId());
        }
        // 4.封装VO
        QuestionVO vo = BeanUtils.copyBean(question, QuestionVO.class);
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }
        return vo;
    }

    /**
     * 删除问题
     * @param id 问题id
     */
    @Override
    @Transactional
    public void deleteQuestion(Long id) {
        // 1. 查询问题是否存在
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }

        // 2. 判断是否是当前用户提问的
        Long currentUserId = UserContext.getUser();
        if (!question.getUserId().equals(currentUserId)) {
            throw new BadRequestException("只能删除自己提出的问题");
        }

        // 3. 删除问题本身
        removeById(id);

        // 4. 查询并删除该问题下的所有回答
        LambdaQueryWrapper<InteractionReply> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(InteractionReply::getQuestionId, id);
        List<InteractionReply> replies = replyMapper.selectList(replyWrapper);

        if (CollUtils.isNotEmpty(replies)) {
            // 4.1. 删除回答下的评论（如果有评论表）
            // 如果有评论表，需要先删除评论
            // 例如：commentMapper.deleteByReplyIds(replyIds);

            // 4.2. 删除所有回答
            replyMapper.delete(replyWrapper);
        }
    }


    /**
     * 管理员查询问题
     * @param query 查询条件
     * @return 问题列表
     */


    @Override
    public PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query) {
        // 1.处理课程名称，得到课程id
        List<Long> courseIds = null;
        if (StringUtils.isNotBlank(query.getCourseName())) {
            courseIds = searchClient.queryCoursesIdByName(query.getCourseName());
            if (CollUtils.isEmpty(courseIds)) {
                return PageDTO.empty(0L, 0L);
            }
        }
        // 2.分页查询
        Integer status = query.getStatus();
        LocalDateTime begin = query.getBeginTime();
        LocalDateTime end = query.getEndTime();
        Page<InteractionQuestion> page = lambdaQuery()
                .in(courseIds != null, InteractionQuestion::getCourseId, courseIds)
                .eq(status != null, InteractionQuestion::getStatus, status)
                .gt(begin != null, InteractionQuestion::getCreateTime, begin)
                .lt(end != null, InteractionQuestion::getCreateTime, end)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }

        // 3.准备VO需要的数据：用户数据、课程数据、章节数据
        Set<Long> userIds = new HashSet<>();
        Set<Long> cIds = new HashSet<>();
        Set<Long> cataIds = new HashSet<>();
        // 3.1.获取各种数据的id集合
        for (InteractionQuestion q : records) {
            userIds.add(q.getUserId());
            cIds.add(q.getCourseId());
            cataIds.add(q.getChapterId());
            cataIds.add(q.getSectionId());
        }
        // 3.2.根据id查询用户
        List<UserDTO> users = userClient.queryUserByIds(userIds);
        Map<Long, UserDTO> userMap = new HashMap<>(users.size());
        if (CollUtils.isNotEmpty(users)) {
            userMap = users.stream().collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 3.3.根据id查询课程
        List<CourseSimpleInfoDTO> cInfos = courseClient.getSimpleInfoList(cIds);
        Map<Long, CourseSimpleInfoDTO> cInfoMap = new HashMap<>(cInfos.size());
        if (CollUtils.isNotEmpty(cInfos)) {
            cInfoMap = cInfos.stream().collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        }

        // 3.4.根据id查询章节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(cataIds);
        Map<Long, String> cataMap = new HashMap<>(catas.size());
        if (CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }


        // 4.封装VO
        List<QuestionAdminVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion q : records) {
            // 4.1.将PO转VO，属性拷贝
            QuestionAdminVO vo = BeanUtils.copyBean(q, QuestionAdminVO.class);
            voList.add(vo);
            // 4.2.用户信息
            UserDTO user = userMap.get(q.getUserId());
            if (user != null) {
                vo.setUserName(user.getName());
            }
            // 4.3.课程信息以及分类信息
            CourseSimpleInfoDTO cInfo = cInfoMap.get(q.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setCategoryName(categoryCache.getCategoryNames(cInfo.getCategoryIds()));
            }
            // 4.4.章节信息
            vo.setChapterName(cataMap.getOrDefault(q.getChapterId(), ""));
            vo.setSectionName(cataMap.getOrDefault(q.getSectionId(), ""));
        }
        return PageDTO.of(page, voList);
    }


    /**
     * 管理端根据id查询问题详情
     * @param id 问题id
     * @return 问题详情
     */
    @Override
    public QuestionAdminVO queryQuestionDetailForAdmin(Long id) {
        // 1. 查询问题
        InteractionQuestion question = getById(id);
        if (question == null) {
            throw new BadRequestException("问题不存在");
        }

        // 2. 查询用户信息
        UserDTO user = userClient.queryUserById(question.getUserId());

        // 3. 查询课程信息
        List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(
                Collections.singletonList(question.getCourseId())
        );
        CourseSimpleInfoDTO courseInfo = CollUtils.isEmpty(courseInfos) ? null : courseInfos.get(0);

        // 4. 查询章节信息
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(
                Arrays.asList(question.getChapterId(), question.getSectionId())
        );
        Map<Long, String> cataMap = catas.stream()
                .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));

        // 5. 查询回答数量
        LambdaQueryWrapper<InteractionReply> replyWrapper = new LambdaQueryWrapper<>();
        replyWrapper.eq(InteractionReply::getQuestionId, id);
        Long replyCount = Long.valueOf(replyMapper.selectCount(replyWrapper));

// 6. 封装VO
        QuestionAdminVO vo = BeanUtils.copyBean(question, QuestionAdminVO.class);

// 6.1 用户信息
        if (user != null) {
            vo.setUserName(user.getName());
            vo.setUserIcon(user.getIcon());
        }

// 6.2 课程信息
        if (courseInfo != null) {
            vo.setCourseName(courseInfo.getName());
            // 课程分类
            vo.setCategoryName(categoryCache.getCategoryNames(courseInfo.getCategoryIds()));
            // 课程讲师
            if (StringUtils.isNotBlank(courseInfo.getTeacherName())) {
                vo.setTeacherName(courseInfo.getTeacherName());
            }
        }

// 6.3 小节信息
        vo.setChapterName(cataMap.getOrDefault(question.getChapterId(), ""));
        vo.setSectionName(cataMap.getOrDefault(question.getSectionId(), ""));

// 6.4 回答次数
        vo.setAnswerTimes(replyCount == null ? 0 : replyCount.intValue());

        return vo;
    }

}
