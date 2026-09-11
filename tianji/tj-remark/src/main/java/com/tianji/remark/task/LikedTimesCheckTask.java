package com.tianji.remark.task;

import com.tianji.remark.properties.LikeProperties;
import com.tianji.remark.service.ILikedRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LikedTimesCheckTask {

    private static final int MAX_BIZ_SIZE = 30;

    private final ILikedRecordService recordService;
    private final LikeProperties likeProperties;

    @Scheduled(fixedDelay = 20000)
    public void checkLikedTimes() {
        for (String bizType : likeProperties.getBizTypes()) {
            recordService.readLikedTimesAndSendMessage(bizType, MAX_BIZ_SIZE);
        }
    }
}