//package com.tianji.remark.service.impl;
//
//import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
//import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
//import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
//import com.tianji.api.dto.remark.LikedTimesDTO;
//import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
//import com.tianji.common.utils.CollUtils;
//import com.tianji.common.utils.StringUtils;
//import com.tianji.common.utils.UserContext;
//import com.tianji.remark.constants.RedisConstants;
//import com.tianji.remark.domain.dto.LikeRecordFormDTO;
//import com.tianji.remark.domain.po.LikedRecord;
//import com.tianji.remark.mapper.LikedRecordMapper;
//import com.tianji.remark.service.ILikedRecordService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.data.redis.core.RedisTemplate;
//import org.springframework.data.redis.core.ZSetOperations;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
//import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;
//
///**
// * <p>
// * 点赞记录表 服务实现类
// * </p>
// */
//@Service
//@RequiredArgsConstructor
//public class LikedRecordServiceImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {
//
//    private final RabbitMqHelper mqHelper;
//
//    private final RedisTemplate<String, Object> redisTemplate;
//    /**
//     * 添加点赞记录
//     *
//     * @param recordDTO 点赞记录
//     */
//    @Override
//    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
//        // 1.基于前端的参数，判断是执行点赞还是取消点赞
//        boolean success = recordDTO.getLiked() ? like(recordDTO) : unlike(recordDTO);
//        // 2.判断是否执行成功，如果失败，则直接结束
//        if (!success) {
//            return;
//        }
//        // 3.如果执行成功，统计点赞总数
//        Integer likedTimes = lambdaQuery()
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .count();
//        // 4.发送MQ通知
//        mqHelper.send(
//                LIKE_RECORD_EXCHANGE,
//                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, recordDTO.getBizType()),
//                LikedTimesDTO.of(recordDTO.getBizId(), likedTimes));
//    }
//
//    private boolean unlike(LikeRecordFormDTO recordDTO) {
//        return remove(new QueryWrapper<LikedRecord>().lambda()
//                .eq(LikedRecord::getUserId, UserContext.getUser())
//                .eq(LikedRecord::getBizId, recordDTO.getBizId()));
//    }
//
//    private boolean like(LikeRecordFormDTO recordDTO) {
//        Long userId = UserContext.getUser();
//
//        // 1.查询点赞记录
//        Integer count = lambdaQuery()
//                .eq(LikedRecord::getUserId, userId)
//                .eq(LikedRecord::getBizId, recordDTO.getBizId())
//                .count();
//        // 2.判断是否存在，如果已经存在，直接结束
//        if (count > 0) {
//            return false;
//        }
//        // 3.如果不存在，直接新增
//        LikedRecord r = new LikedRecord();
//        r.setUserId(userId);
//        r.setBizId(recordDTO.getBizId());
//        r.setBizType(recordDTO.getBizType());
//        save(r);
//        return true;
//    }
//
//    /**
//     * 判断业务是否被当前用户点赞
//     *
//     * @param bizIds 业务id
//     * @return 业务id集合
//     */
//    @Override
//    public Set<Long> isBizLiked(List<Long> bizIds) {
//        Long userId = UserContext.getUser();
//        Set<Long> likedBizIds = new HashSet<>();
//
//        for (Long bizId : bizIds) {
//            String key = RedisConstants.LIKE_BIZ_KEY_PREFIX + bizId;
//            Boolean member = redisTemplate.opsForSet().isMember(key, userId.toString());
//
//            if (Boolean.TRUE.equals(member)) {
//                likedBizIds.add(bizId);
//            } else {
//                // Redis 没有，查数据库兜底
//                LikedRecord record = lambdaQuery()
//                        .eq(LikedRecord::getBizId, bizId)
//                        .eq(LikedRecord::getUserId, userId)
//                        .one();
//                if (record != null) {
//                    likedBizIds.add(bizId);
//                    // 回填 Redis
//                    redisTemplate.opsForSet().add(key, userId.toString());
//                }
//            }
//        }
//        return likedBizIds;
//    }
//
//    /**
//     * 批量读取点赞数并发送MQ消息
//     *
//     * @param bizType 业务类型
//     * @param maxBizSize 最大业务数
//     */
//    @Override
//    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
//        // 1.读取并移除Redis中缓存的点赞总数
//        String key = RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType;
//        Set<ZSetOperations.TypedTuple<Object>> tuples = redisTemplate.opsForZSet().popMin(key, maxBizSize);
//        if (CollUtils.isEmpty(tuples)) {
//            return;
//        }
//        // 2.数据转换
//        List<LikedTimesDTO> list = new ArrayList<>(tuples.size());
//        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
//            Object bizIdObj = tuple.getValue();
//            Double likedTimes = tuple.getScore();
//            if (bizIdObj == null || likedTimes == null) {
//                continue;
//            }
//            list.add(new LikedTimesDTO(Long.valueOf(bizIdObj.toString()), likedTimes.intValue()));
//        }
//        // 3.发送MQ消息
//        mqHelper.send(
//                LIKE_RECORD_EXCHANGE,
//                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, bizType),
//                list);
//    }
//
//
//
//
//    @Override
//    public void persistLikedRecordsToDb() {
//        Set<String> keys = redisTemplate.keys(RedisConstants.LIKE_BIZ_KEY_PREFIX + "*");
//        if (CollUtils.isEmpty(keys)) {
//            return;
//        }
//
//        for (String key : keys) {
//            Long bizId = Long.valueOf(key.replace(RedisConstants.LIKE_BIZ_KEY_PREFIX, ""));
//            Set<Object> userIds = redisTemplate.opsForSet().members(key);
//
//            if (CollUtils.isEmpty(userIds)) {
//                continue;
//            }
//
//            List<LikedRecord> records = new ArrayList<>();
//            for (Object userIdStr : userIds) {
//                LikedRecord record = new LikedRecord();
//                record.setUserId(Long.valueOf(userIdStr.toString()));
//                record.setBizId(bizId);
//                record.setBizType(resolveBizType(key)); // 根据 key 规则或额外存储解析
//                records.add(record);
//            }
//
//            // 先删除该 bizId 下旧记录，再批量插入
//            remove(new LambdaQueryWrapper<LikedRecord>().eq(LikedRecord::getBizId, bizId));
//            saveBatch(records);
//        }
//    }
//}