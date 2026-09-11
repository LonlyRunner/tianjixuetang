package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domian.po.LearningLesson;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domian.vo.LearningLessonVO;
import com.tianji.learning.domian.vo.LearningPlanPageVO;

import java.util.List;

/**
 * <p>
 * 学生课程表 服务类
 * </p>
 *
 * @author wyy
 */
public interface ILearningLessonService extends IService<LearningLesson> {

    /**
     * 添加用户课程到课表
     * @param userId 用户id
     * @param courseIds 课程id列表
     */
    void addUserLessons(Long userId, List<Long> courseIds);

    /**
     * 删除用户课表中的指定课程（退款场景）
     * @param userId 用户id
     * @param courseIds 课程id列表
     */
    void deleteUserLessons(Long userId, List<Long> courseIds);

    /**
     * 删除当前用户课表中已失效的指定课程
     * @param courseId 课程id
     */
    void deleteUserLessonByCourseId(Long courseId);

    /**
     * 分页查询我的课表
     * @param query 分页查询
     * @return 我的课表
     */
    PageDTO<LearningLessonVO> queryMyLessons(PageQuery query);

    /**
     * 查询当前正在学习的课程
     * @return 课程信息
     */
    LearningLessonVO queryMyCurrentLesson();

    /**
     * 校验当前用户是否可以学习当前课程
     * @param courseId 课程id
     * @return lessonId，如果报名了则返回lessonId，否则返回空
     */
    Long isLessonValid(Long courseId);

    /**
     * 查询当前用户课表中指定课程状态
     * @param courseId 课程id
     * @return 课表信息
     */
    LearningLessonVO queryLessonByCourseId(Long courseId);

    /**
     * 统计课程学习人数
     * @param courseId 课程id
     * @return 学习人数
     */
    Integer countLearningLessonByCourse(Long courseId);


    /**
     * 根据用户id和课程id查询课表
     * @param userId 用户id
     * @param courseId 课程id
     * @return 课表信息
     */
    LearningLesson queryByUserAndCourseId(Long userId, Long courseId);

    /**
     * 创建用户学习计划
     * @param courseId 课程id
     * @param freq 频率
     */
    void createLearningPlan(Long courseId, Integer freq);

    /**
     * 分页查询我的学习计划
     * @param query 分页查询
     * @return 我的学习计划
     */
    LearningPlanPageVO queryMyPlans(PageQuery query);
}
