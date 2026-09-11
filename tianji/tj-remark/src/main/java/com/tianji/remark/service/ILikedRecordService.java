package com.tianji.remark.service;

import com.tianji.remark.domain.dto.LikeRecordFormDTO;
import com.tianji.remark.domain.po.LikedRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * 点赞记录表 服务类
 * </p>
 *
 * @author wyy
 */
public interface ILikedRecordService extends IService<LikedRecord> {


    void addLikeRecord(LikeRecordFormDTO recordFormDTO);

    Set<Long> isBizLiked(List<Long> bizIds);

    void readLikedTimesAndSendMessage(String bizType, int maxBizSize);

    /**
     * 将 Redis 中的点赞记录同步到数据库
     */
    void persistLikedRecordsToDb();
}
