package com.tianji.learning.task;

import com.tianji.learning.domian.po.LearningLesson;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 课程过期检查任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LessonExpirationTask {

    private final ILearningLessonService lessonService;

    /**
     * 每天凌晨2点检查一次过期课程
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkExpiredLessons() {
        log.debug("开始检查过期课程...");
        // 直接一条SQL更新：expire_time < now 且 状态不是已过期
        boolean success = lessonService.lambdaUpdate()
                .set(LearningLesson::getStatus, LessonStatus.EXPIRED.getValue())
                .lt(LearningLesson::getExpireTime, LocalDateTime.now())
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED.getValue())
                .update();
        if (!success) {
            log.error("更新过期课程状态失败");
        } else {
            log.debug("过期课程状态更新完成");
        }
    }
}