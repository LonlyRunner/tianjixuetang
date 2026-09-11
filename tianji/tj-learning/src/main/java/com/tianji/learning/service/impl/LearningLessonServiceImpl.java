package com.tianji.learning.service.impl;


import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.learning.domian.dto.LearningRecordFormDTO;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.IdAndNumDTO;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.*;
import com.tianji.learning.domian.po.LearningLesson;
import com.tianji.learning.domian.po.LearningRecord;
import com.tianji.learning.domian.vo.LearningLessonVO;
import com.tianji.learning.domian.vo.LearningPlanPageVO;
import com.tianji.learning.domian.vo.LearningPlanVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.enums.PlanStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.mq.message.PointsRecordMessage;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author wyy
 */
@SuppressWarnings("ALL")
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {


    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final LearningRecordMapper recordMapper;

    private final RabbitMqHelper mqHelper;



    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        // 1.查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cInfoList)) {
            // 课程信息不存在，无法添加
            log.error("课程信息不存在，无法添加到课表");
            return;
        }
        // 2.循环遍历，封装成LearningLesson对象
        List<LearningLesson> list = new ArrayList<>(cInfoList.size());
        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            LearningLesson lesson = new LearningLesson();
            // 2.1.获取有效时间
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration != null && validDuration > 0) {
                LocalDateTime now = LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
            // 2.2.填充userId和courseId
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);
        }
        // 3.批量保存
        saveBatch(list);
    }

    @Override
    @Transactional
    public void deleteUserLessons(Long userId, List<Long> courseIds) {
        // 1.参数校验
        if (userId == null || CollUtils.isEmpty(courseIds)) {
            log.error("删除课表参数为空，无法删除");
            return;
        }
        // 2.删除用户课表中的指定课程
        lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .in(LearningLesson::getCourseId, courseIds)
                .remove();
    }

    @Override
    @Transactional
    public void deleteUserLessonByCourseId(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null || courseId == null) {
            return;
        }
        // 2.删除当前用户课表中已失效的指定课程
        lambdaUpdate()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .eq(LearningLesson::getStatus, LessonStatus.EXPIRED.getValue())
                .remove();
    }

    /**
     * 分页查询我的课表
     *
     * @param query 分页查询
     * @return 我的课表
     */
    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.分页查询
        // select * from learning_lesson where user_id = #{userId} order by latest_learn_time limit 0, 5
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId) // where user_id = #{userId}
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.查询课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);

        // 4.封装VO返回
        List<LearningLessonVO> list = new ArrayList<>(records.size());
        // 4.1.循环遍历，将LearningLesson转为VO
        for (LearningLesson r : records) {
            // 4.2.拷贝基础属性到vo
            LearningLessonVO vo = BeanUtils.copyBean(r, LearningLessonVO.class);
            // 4.3.获取课程信息，填充到vo
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            list.add(vo);
        }
        return PageDTO.of(page, list);
    }



    /**
     * 查询当前正在学习的课程
     *
     * @return 当前正在学习的课程
     */

    @Override
    public LearningLessonVO queryMyCurrentLesson() {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.查询正在学习的课程 select * from xx where user_id = #{userId} AND status = 1 order by latest_learn_time limit 1
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null) {
            return null;
        }
        // 3.拷贝PO对象基础属性到VO
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        // 4.查询课程信息
        CourseFullInfoDTO cInfo = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cInfo == null) {
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(cInfo.getName());
        vo.setCourseCoverUrl(cInfo.getCoverUrl());
        vo.setSections(cInfo.getSectionNum());
        // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount);
        // 6.查询小节信息
        List<CataSimpleInfoDTO> cataInfos =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)) {
            CataSimpleInfoDTO cataInfo = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
    }
    /**
     * 查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @Override
    public Long isLessonValid(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null || courseId == null) {
            return null;
        }
        // 2.查询课表，要求课程有效（未过期且状态不是已过期）
        // select * from learning_lesson
        // where user_id = #{userId} and course_id = #{courseId}
        // and status != 3 and (expire_time is null or expire_time > now())
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .ne(LearningLesson::getStatus, LessonStatus.EXPIRED.getValue())
                .and(i -> i.isNull(LearningLesson::getExpireTime)
                        .or()
                        .gt(LearningLesson::getExpireTime, LocalDateTime.now()))
                .one();
        // 3.返回lessonId
        return lesson == null ? null : lesson.getId();
    }
    /**
     * 查询课程信息
     *
     * @param courseId 课程id
     * @return 课程信息
     */
    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        if (userId == null || courseId == null) {
            return null;
        }
        // 2.查询课表
        LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
        if (lesson == null) {
            return null;
        }
        // 3.拷贝属性返回
        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }
    /**
     * 查询课程数量
     *
     * @param courseId 课程id
     * @return 课程数量
     */
    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        if (courseId == null) {
            return 0;
        }
        return lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .count();
    }
    /**
     * 查询用户课程表
     *
     * @param query 查询参数
     * @return 用户课程表
     */
    @Override
    public LearningLesson queryByUserAndCourseId(Long userId, Long courseId) {
        if (userId == null || courseId == null) {
            return null;
        }
        return lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId)
                .one();
    }
    /**
     * 创建学习计划
     *
     * @param courseId 课程id
     * @param freq     周学习频率
     */
    @Override
    public void createLearningPlan(Long courseId, Integer freq) {
        // 1.获取当前登录的用户
        Long userId = UserContext.getUser();
        // 2.查询课表中的指定课程有关的数据
        LearningLesson lesson = queryByUserAndCourseId(userId, courseId);
        AssertUtils.isNotNull(lesson, "课程信息不存在！");
        // 3.修改数据
        LearningLesson l = new LearningLesson();
        l.setId(lesson.getId());
        l.setWeekFreq(freq);
        if(lesson.getPlanStatus() == PlanStatus.NO_PLAN) {
            l.setPlanStatus(PlanStatus.PLAN_RUNNING);
        }
        updateById(l);
    }

    /**
     * 查询我的课程表
     *
     * @param query 分页参数
     * @return 我的课程表
     */
    @Override
    public LearningPlanPageVO queryMyPlans(PageQuery query) {
        LearningPlanPageVO result = new LearningPlanPageVO();
        // 1.获取当前登录用户
        Long userId = UserContext.getUser();
        // 2.获取本周起始时间
        LocalDate now = LocalDate.now();
        LocalDateTime begin = DateUtils.getWeekBeginTime(now);
        LocalDateTime end = DateUtils.getWeekEndTime(now);
        // 3.查询总的统计数据
        // 3.1.本周总的已学习小节数量
        Integer weekFinished = recordMapper.selectCount(new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getFinished, true)
                .gt(LearningRecord::getFinishTime, begin)
                .lt(LearningRecord::getFinishTime, end)
        );
        result.setWeekFinished(weekFinished);
        // 3.2.本周总的计划学习小节数量
        Integer weekTotalPlan = getBaseMapper().queryTotalPlan(userId);
        result.setWeekTotalPlan(weekTotalPlan);
        // TODO 3.3.本周学习积分

        // 4.查询分页数据
        // 4.1.分页查询课表信息以及学习计划信息
        Page<LearningLesson> p = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getPlanStatus, PlanStatus.PLAN_RUNNING)
                .in(LearningLesson::getStatus, LessonStatus.NOT_BEGIN, LessonStatus.LEARNING)
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = p.getRecords();
        if (CollUtils.isEmpty(records)) {
            return result.pageInfo(p.getTotal(), p.getPages(), CollUtils.emptyList());
        }
        // 4.2.查询课表对应的课程信息
        Map<Long, CourseSimpleInfoDTO> cMap = queryCourseSimpleInfoList(records);
        // 4.3.统计每一个课程本周已学习小节数量
        List<IdAndNumDTO> list = recordMapper.countLearnedSections(userId, begin, end);
        Map<Long, Integer> countMap = IdAndNumDTO.toMap(list);
        // 4.4.组装数据VO
        List<LearningPlanVO> voList = new ArrayList<>(records.size());
        for (LearningLesson r : records) {
            // 4.4.1.拷贝基础属性到vo
            LearningPlanVO vo = BeanUtils.copyBean(r, LearningPlanVO.class);
            // 4.4.2.填充课程详细信息
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            if (cInfo != null) {
                vo.setCourseName(cInfo.getName());
                vo.setSections(cInfo.getSectionNum());
            }
            // 4.4.3.每个课程的本周已学习小节数量
            vo.setWeekLearnedSections(countMap.getOrDefault(r.getId(), 0));
            voList.add(vo);
        }
        return result.pageInfo(p.getTotal(), p.getPages(), voList);
    }


    /**
     * 查询课程信息
     *
     * @param records 课程id集合
     * @return 课程信息
     */
    private Map<Long, CourseSimpleInfoDTO> queryCourseSimpleInfoList(List<LearningLesson> records) {
        // 3.1.获取课程id
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        // 3.2.查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList)) {
            // 课程不存在，无法返回
            throw new BadRequestException("课程信息不存在！");
        }
        // 3.3.把课程集合转换为Map，key是courseId，值是course对象
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));
        return cMap;
    }


}
