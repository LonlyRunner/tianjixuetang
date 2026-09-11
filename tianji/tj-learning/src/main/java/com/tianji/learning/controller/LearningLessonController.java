package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domian.dto.LearningPlanDTO;
import com.tianji.learning.domian.vo.LearningLessonVO;
import com.tianji.learning.domian.vo.LearningPlanPageVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author wyy
 */
@Api(tags = "LearningLesson控制器")
@RestController
@RequiredArgsConstructor
@RequestMapping("/lessons")
public class LearningLessonController {

    private final ILearningLessonService lessonService;

    /**
     * 查询我的课表
     * @param query 分页查询
     * @return 我的课表
     */
    @ApiOperation("查询我的课表，排序字段 latest_learn_time:学习时间倒序，create_time:创建时间倒序")
    @GetMapping("/page")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        return lessonService.queryMyLessons(query);
    }

    /**
     * 查询当前正在学习的课程
     * @return 课程信息
     */
    @GetMapping("/now")
    @ApiOperation("查询当前正在学习的课程")
    public LearningLessonVO queryMyCurrentLesson() {
        return lessonService.queryMyCurrentLesson();
    }

    /**
     * 删除课表中已失效的课程
     * @param courseId 课程id
     */
    @ApiOperation("删除课表中已失效的课程")
    @DeleteMapping("/{courseId}")
    public void deleteLessonByCourseId(@PathVariable("courseId") Long courseId) {
        lessonService.deleteUserLessonByCourseId(courseId);
    }

    /**
     * 校验当前用户是否可以学习当前课程
     * @param courseId 课程id
     * @return lessonId，如果报名了则返回lessonId，否则返回空
     */
    @ApiOperation("校验当前用户是否可以学习当前课程")
    @GetMapping("/{courseId}/valid")
    public Long isLessonValid(@PathVariable("courseId") Long courseId) {
        return lessonService.isLessonValid(courseId);
    }

    /**
     * 查询用户课表中指定课程状态
     * @param courseId 课程id
     * @return 课表信息
     */
    @ApiOperation("查询用户课表中指定课程状态")
    @GetMapping("/{courseId}")
    public LearningLessonVO queryLessonByCourseId(@PathVariable("courseId") Long courseId) {
        return lessonService.queryLessonByCourseId(courseId);
    }

    /**
     * 统计课程学习人数
     * @param courseId 课程id
     * @return 学习人数
     */
    @ApiOperation("统计课程学习人数")
    @GetMapping("/{courseId}/count")
    public Integer countLearningLessonByCourse(@PathVariable("courseId") Long courseId) {
        return lessonService.countLearningLessonByCourse(courseId);
    }

    @ApiOperation("创建学习计划")
    @PostMapping("/plans")
    public void createLearningPlans(@Valid @RequestBody LearningPlanDTO planDTO){
        lessonService.createLearningPlan(planDTO.getCourseId(), planDTO.getFreq());
    }

    @ApiOperation("查询我的学习计划")
    @GetMapping("/plans")
    public LearningPlanPageVO queryMyPlans(PageQuery query){
        return lessonService.queryMyPlans(query);
    }
}
