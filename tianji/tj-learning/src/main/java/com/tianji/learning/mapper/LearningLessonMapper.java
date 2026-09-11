package com.tianji.learning.mapper;

import com.tianji.learning.domian.po.LearningLesson;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * <p>
 * 学生课程表 Mapper 接口
 * </p>
 *
 * @author wyy
 */
public interface LearningLessonMapper extends BaseMapper<LearningLesson> {

    Integer queryTotalPlan(Long userId);
}
