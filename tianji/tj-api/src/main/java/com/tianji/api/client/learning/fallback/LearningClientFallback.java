package com.tianji.api.client.learning.fallback;

import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningLessonStatusDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

@Slf4j
public class LearningClientFallback implements FallbackFactory<LearningClient> {

    @Override
    public LearningClient create(Throwable cause) {
        log.error("调用学习服务异常", cause);
        return new LearningClient() {
            @Override
            public Integer countLearningLessonByCourse(Long courseId) {
                return 0;
            }

            @Override
            public Long isLessonValid(Long courseId) {
                return null;
            }

            @Override
            public LearningLessonDTO queryLessonByCourse(Long courseId) {
                return null;
            }


            @Override
            public LearningLessonDTO queryLearningRecordByCourse(Long courseId) {
                log.error("queryLearningRecordByCourse failed: {}", cause.getMessage());
                return null;
            }
        };
    }
}
