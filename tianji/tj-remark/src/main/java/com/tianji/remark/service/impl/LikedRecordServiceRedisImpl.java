package com.tianji.remark.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.remark.constants.RedisConstants;
import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.tianji.remark.mapper.LikedRecordMapper;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.StringRedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class LikedRecordServiceRedisImpl extends ServiceImpl<LikedRecordMapper, LikedRecord> implements ILikedRecordService {

    private final RabbitMqHelper mqHelper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void addLikeRecord(LikeRecordFormDTO recordDTO) {
        boolean success = recordDTO.getLiked() ? like(recordDTO) : unlike(recordDTO);
        if (!success) {
            return;
        }
        Long likedTimes = redisTemplate.opsForSet()
                .size(buildLikeKey(recordDTO.getBizType(), recordDTO.getBizId()));
        if (likedTimes == null) {
            return;
        }
        redisTemplate.opsForZSet().add(
                RedisConstants.LIKES_TIMES_KEY_PREFIX + recordDTO.getBizType(),
                recordDTO.getBizId().toString(),
                likedTimes
        );
    }

    private String buildLikeKey(String bizType, Long bizId) {
        return RedisConstants.LIKE_BIZ_KEY_PREFIX + bizType + ":" + bizId;
    }

    private boolean unlike(LikeRecordFormDTO recordDTO) {
        Long userId = UserContext.getUser();
        String key = buildLikeKey(recordDTO.getBizType(), recordDTO.getBizId());
        Long result = redisTemplate.opsForSet().remove(key, userId.toString());
        return result != null && result > 0;
    }

    private boolean like(LikeRecordFormDTO recordDTO) {
        Long userId = UserContext.getUser();
        String key = buildLikeKey(recordDTO.getBizType(), recordDTO.getBizId());
        Long result = redisTemplate.opsForSet().add(key, userId.toString());
        return result != null && result > 0;
    }

    @Override
    public Set<Long> isBizLiked(List<Long> bizIds) {
        Long userId = UserContext.getUser();
        // 注意：这里需要知道 bizType 才能构造 key
        // 如果业务上这里只查一种类型，可以传参进来；下面给的是通用思路
        List<Object> objects = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            StringRedisConnection src = (StringRedisConnection) connection;
            for (Long bizId : bizIds) {
                // 这里假设默认业务类型是 QA，实际应该根据业务场景传入
                String key = buildLikeKey("QA", bizId);
                src.sIsMember(key, userId.toString());
            }
            return null;
        });
        return IntStream.range(0, objects.size())
                .filter(i -> (boolean) objects.get(i))
                .mapToObj(bizIds::get)
                .collect(Collectors.toSet());
    }

    @Override
    public void readLikedTimesAndSendMessage(String bizType, int maxBizSize) {
        String key = RedisConstants.LIKES_TIMES_KEY_PREFIX + bizType;
        Set<ZSetOperations.TypedTuple<Object>> tuples = (Set<ZSetOperations.TypedTuple<Object>>) redisTemplate.opsForZSet().popMin(key, Duration.ofDays(maxBizSize));
        if (CollUtils.isEmpty(tuples)) {
            return;
        }
        List<LikedTimesDTO> list = new ArrayList<>(tuples.size());
        for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
            Object bizIdObj = tuple.getValue();
            Double likedTimes = tuple.getScore();
            if (bizIdObj == null || likedTimes == null) {
                continue;
            }
            list.add(new LikedTimesDTO(Long.valueOf(bizIdObj.toString()), likedTimes.intValue()));
        }
        mqHelper.send(
                LIKE_RECORD_EXCHANGE,
                StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, bizType),
                list);
    }

    /**
     * 将 Redis 点赞记录持久化到数据库
     */
    @Override
    public void persistLikedRecordsToDb() {
        // 扫描 likes:biz:* 下的所有 key
        Set<String> keys = redisTemplate.keys(RedisConstants.LIKE_BIZ_KEY_PREFIX + "*");
        if (CollUtils.isEmpty(keys)) {
            return;
        }

        for (String key : keys) {
            String[] parts = key.split(":");
            if (parts.length < 4) {
                continue;
            }
            String bizType = parts[2];
            Long bizId = Long.valueOf(parts[3]);

            Set<String> userIds = redisTemplate.opsForSet().members(key);
            if (CollUtils.isEmpty(userIds)) {
                continue;
            }

            // 先删除该 bizId 下的旧记录
            remove(new LambdaQueryWrapper<LikedRecord>()
                    .eq(LikedRecord::getBizId, bizId)
                    .eq(LikedRecord::getBizType, bizType));

            // 批量插入新记录
            List<LikedRecord> records = userIds.stream()
                    .map(userIdStr -> {
                        LikedRecord record = new LikedRecord();
                        record.setUserId(Long.valueOf(userIdStr));
                        record.setBizId(bizId);
                        record.setBizType(bizType);
                        return record;
                    })
                    .collect(Collectors.toList());

            saveBatch(records);
        }
    }
}