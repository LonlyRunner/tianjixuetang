package com.tianji.exam.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.api.dto.course.CatalogueDTO;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.client.learning.LearningClient;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.api.dto.exam.QuestionDTO;
import com.tianji.api.dto.leanring.LearningLessonDTO;
import com.tianji.api.dto.leanring.LearningRecordDTO;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.autoconfigure.mq.RabbitMqHelper;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.exam.domain.dto.ExamCommitDTO;
import com.tianji.exam.domain.dto.ExamDetailDTO;
import com.tianji.exam.domain.dto.ExamFormDTO;
import com.tianji.exam.domain.po.ExamRecord;
import com.tianji.exam.domain.po.ExamRecordDetail;
import com.tianji.exam.domain.query.ExamPageQuery;
import com.tianji.exam.domain.vo.ExamQuestionVO;
import com.tianji.exam.domain.vo.ExamRecordAdminVO;
import com.tianji.exam.domain.vo.ExamRecordDetailVO;
import com.tianji.exam.domain.vo.ExamRecordVO;
import com.tianji.exam.enums.ExamType;
import com.tianji.exam.mapper.ExamRecordMapper;
import com.tianji.exam.service.IExamRecordDetailService;
import com.tianji.exam.service.IExamRecordService;
import com.tianji.exam.service.IQuestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * <p>
 * 考试记录表 服务实现类
 * </p>
 *
 * @author 虎哥
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ExamRecordServiceImpl extends ServiceImpl<ExamRecordMapper, ExamRecord> implements IExamRecordService {

    private final LearningClient learningClient;

    private final IQuestionService questionService;

    private final IExamRecordDetailService recordDetailService;

    private final CourseClient courseClient;

    private final CatalogueClient catalogueClient;

    private final UserClient userClient;

    private final RabbitMqHelper mqHelper;

    @Override
    public PageDTO<ExamRecordVO> queryMyExamRecordsPage(PageQuery query) {
        // 1.获取用户
        Long userId = UserContext.getUser();
        // 2.查询 练习没有分的过滤掉
        Page<ExamRecord> page = lambdaQuery()
                .eq(ExamRecord::getUserId, userId)
                .eq(ExamRecord::getFinished, true)
                .page(query.toMpPage("finish_time", false));
        List<ExamRecord> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.获取课程和章节信息
        Set<Long> courseIds = new HashSet<>();
        Set<Long> secIds = new HashSet<>();
        // 3.1.获取课程和小节id
        for (ExamRecord r : records) {
            courseIds.add(r.getCourseId());
            secIds.add(r.getSectionId());
        }
        // 3.2.查询课程
        List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
        Map<Long, String> courseMap = new HashMap<>(0);
        if(CollUtils.isNotEmpty(courseInfos)) {
            courseMap = courseInfos.stream()
                    .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
        }
        // 3.3.查询小节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(secIds);
        Map<Long, String> cataMap = new HashMap<>(0);
        if(CollUtils.isNotEmpty(catas)) {
           cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        // 4.结果处理
        List<ExamRecordVO> list = new ArrayList<>(records.size());
        for (ExamRecord r : records) {
            ExamRecordVO v = BeanUtils.toBean(r, ExamRecordVO.class);
            v.setCourseName(courseMap.getOrDefault(r.getCourseId(), "未知"));
            v.setSectionName(cataMap.getOrDefault(r.getSectionId(), "未知"));
            list.add(v);
        }
        return new PageDTO<>(page.getTotal(), page.getPages(), list);
    }

    @Override
    public PageDTO<ExamRecordAdminVO> queryAdminExamRecordsPage(ExamPageQuery query) {
        // 2.查询 练习没有分的过滤掉
        Page<ExamRecord> page = lambdaQuery()
                .eq(query.getType()  != null,ExamRecord::getType, query.getType())
                .eq(ExamRecord::getFinished, true)
                .page(query.toMpPage("finish_time", false));
        List<ExamRecord> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.获取课程和章节信息
        Set<Long> courseIds = new HashSet<>();
        Set<Long> secIds = new HashSet<>();
        // 3.1.获取课程和小节id
        for (ExamRecord r : records) {
            courseIds.add(r.getCourseId());
            secIds.add(r.getSectionId());
        }
        // 3.2.查询课程
        List<CourseSimpleInfoDTO> courseInfos = courseClient.getSimpleInfoList(courseIds);
        Map<Long, String> courseMap = new HashMap<>(0);
        if(CollUtils.isNotEmpty(courseInfos)) {
            courseMap = courseInfos.stream()
                    .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, CourseSimpleInfoDTO::getName));
        }
        // 3.3.查询小节
        List<CataSimpleInfoDTO> catas = catalogueClient.batchQueryCatalogue(secIds);
        Map<Long, String> cataMap = new HashMap<>(0);
        if(CollUtils.isNotEmpty(catas)) {
            cataMap = catas.stream()
                    .collect(Collectors.toMap(CataSimpleInfoDTO::getId, CataSimpleInfoDTO::getName));
        }
        //查询用户信息
        List<UserDTO> users = userClient.queryUserByIds(records.stream()
                .map(ExamRecord::getUserId)
                .collect(Collectors.toSet()));
        Map<Long, UserDTO> userMap = CollUtils.isEmpty(users) ? new HashMap<>() :
                users.stream().collect(Collectors.toMap(UserDTO::getId, Function.identity(), (u1, u2) -> u1));
        // 4.结果处理
        List<ExamRecordAdminVO> list = new ArrayList<>(records.size());
        for (ExamRecord r : records) {
            ExamRecordAdminVO v = BeanUtils.toBean(r, ExamRecordAdminVO.class);
            v.setCourseName(courseMap.getOrDefault(r.getCourseId(), "未知"));
            v.setSectionName(cataMap.getOrDefault(r.getSectionId(), "未知"));
            v.setUserId(r.getUserId());
            UserDTO user = userMap.get(r.getUserId());
            v.setName(user == null ? "未知用户" : user.getName());
            v.setIcon(user == null ? "" : user.getIcon());
            list.add(v);
        }
        return new PageDTO<>(page.getTotal(), page.getPages(), list);
    }

    @Override
    public List<ExamRecordDetailVO> queryAdminDetailsByExamId(Long examId) {
        ExamRecord exam = getById(examId);
        if(exam == null ){
            throw new BadRequestException("考试记录不存在");
        }
        // 1.查询考试详情
        List<ExamRecordDetail> details = recordDetailService.lambdaQuery()
                .eq(ExamRecordDetail::getExamId, examId)
                .list();
        AssertUtils.isNotEmpty(details, "考试数据不存在");
        // 2.获取问题信息
        List<Long> qIds = details.stream().map(ExamRecordDetail::getQuestionId).collect(Collectors.toList());
        // 3.查询问题
        List<QuestionDTO> questions = questionService.queryQuestionByIds(qIds);
        AssertUtils.isTrue(questions.size() == qIds.size(), "问题不存在");
        Map<Long, QuestionDTO> qMap = questions.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, Function.identity()));
        // 4.组织VO
        List<ExamRecordDetailVO> list = new ArrayList<>(details.size());
        for (ExamRecordDetail detail : details) {
            ExamRecordDetailVO v = BeanUtils.toBean(detail, ExamRecordDetailVO.class);
            v.setQuestion(qMap.get(detail.getQuestionId()));
            list.add(v);
        }
        return list;
    }

    @Override
    @Transactional
    public ExamQuestionVO saveExamRecord(ExamFormDTO examFormDTO) {
        // 1.判断当前用户是否购买了课程
        Long lessonId = learningClient.isLessonValid(examFormDTO.getCourseId());
        if (lessonId == null) {
            // 未购买的课程
            throw new BizIllegalException("请先购买课程!");
        }
        // 2.获取用户
        Long userId = UserContext.getUser();
        if (examFormDTO.getType() == ExamType.EXAM) {
            checkStageExamAllowed(examFormDTO);
        } else {
            lambdaUpdate()
                    .eq(ExamRecord::getUserId, userId)
                    .eq(ExamRecord::getSectionId, examFormDTO.getSectionId())
                    .eq(ExamRecord::getType, ExamType.TEST)
                    .eq(ExamRecord::getFinished, false)
                    .remove();
        }
        // 3.查询考试记录
        ExamRecord r = null;
        if (examFormDTO.getType() == ExamType.EXAM) {
            r = lambdaQuery()
                    .eq(ExamRecord::getUserId, userId)
                    .eq(ExamRecord::getSectionId, examFormDTO.getSectionId())
                    .eq(ExamRecord::getType, ExamType.EXAM)
                    .one();
        }
        // 4.判断记录是否已经存在
        if (r != null) {
            // 4.1.已存在，则判断是否考试还是练习
            if (Boolean.TRUE.equals(r.getFinished())) {
                // 4.3.是考试且已经提交，抛出异常
                throw new BadRequestException("不能重复考试！");
            }
        }

        if(r == null){
            // 5.新增考试记录
            r = new ExamRecord();
            r.setUserId(userId);
            r.setCourseId(examFormDTO.getCourseId());
            r.setSectionId(examFormDTO.getSectionId());
            r.setType(examFormDTO.getType());
            save(r);
        }

        // 6.查询考试题目
        List<QuestionDTO> questions = questionService.queryQuestionByBizId(examFormDTO.getSectionId());
        if (CollUtils.isEmpty(questions)) {
            throw new BadRequestException("当前章节未配置题目");
        }
        // 7.封装VO返回
        ExamQuestionVO vo = new ExamQuestionVO();
        vo.setQuestions(questions);
        vo.setId(r.getId());
        return vo;
    }

    @Override
    @Transactional
    public void saveExamRecordDetails(ExamCommitDTO examCommitDTO) {
        // 1.查询考试记录是否存在
        Long recordId = examCommitDTO.getId();
        Long userId = UserContext.getUser();
        log.info("用户{}提交考试记录{}", userId, examCommitDTO);
        ExamRecord record = getById(recordId);
        if (record == null) {
            throw new BadRequestException("考试记录不存在");
        }
        if (!record.getUserId().equals(userId)) {
            log.error("用户{}访问错误的考试记录{}", userId, recordId);
            throw new BadRequestException("错误的考试记录");
        }
        if (Boolean.TRUE.equals(record.getFinished())) {
            throw new BadRequestException("考试记录已经提交，请勿重复提交");
        }
        // 2.自动批阅答案并保存答案信息
        // 2.1.获取答案信息
        List<ExamDetailDTO> examDetails = examCommitDTO.getExamDetails();
        if (CollUtils.isEmpty(examDetails) && record.getType() == ExamType.TEST) {
            throw new BadRequestException("答题信息不能为空");
        }
        if (examDetails == null) {
            examDetails = CollUtils.emptyList();
        }
        Set<Long> allowedQuestionIds = questionService.queryQuestionByBizId(record.getSectionId())
                .stream().map(QuestionDTO::getId).collect(Collectors.toSet());
        Set<Long> submittedQuestionIds = new HashSet<>();
        for (ExamDetailDTO detail : examDetails) {
            if (detail.getQuestionId() == null || !allowedQuestionIds.contains(detail.getQuestionId())) {
                throw new BadRequestException("答题信息包含非本场题目");
            }
            if (!submittedQuestionIds.add(detail.getQuestionId())) {
                throw new BadRequestException("同一道题不能重复提交");
            }
        }
        // 2.2.获取问题id
        List<Long> qIds = examDetails.stream()
                .filter(d -> StrUtil.isNotBlank(d.getAnswer()))
                .map(ExamDetailDTO::getQuestionId)
                .collect(Collectors.toList());
        // 2.3.查询问题
        Map<Long, QuestionDTO> qMap = new HashMap<>(qIds.size());
        if(CollUtils.isNotEmpty(qIds)) {
            List<QuestionDTO> qs = questionService.queryQuestionByIds(qIds);
            qMap = qs.stream().collect(Collectors.toMap(QuestionDTO::getId, s -> s));
        }
        // 2.4.组织考试明细数据
        int score = 0;
        int correct = 0;
        Map<Long, Integer> answerTimes = new HashMap<>();
        Map<Long, Integer> correctTimes = new HashMap<>();
        List<ExamRecordDetail> list = new ArrayList<>(examDetails.size());
        for (ExamDetailDTO dto : examDetails) {
            // 5.1.初始化
            ExamRecordDetail d = new ExamRecordDetail();
            d.setQuestionId(dto.getQuestionId());
            d.setAnswer(dto.getAnswer());
            d.setExamId(recordId);
            answerTimes.put(d.getQuestionId(), 1);
            // 5.2.批阅客观题
            // 获取问题
            QuestionDTO question = qMap.get(d.getQuestionId());
            // 校验是否正确
            if (question != null && StringUtils.equals(question.getAnswer(), d.getAnswer())) {
                d.setCorrect(true);
                d.setScore(question.getScore());
                score += question.getScore();
                correct++;
                correctTimes.put(d.getQuestionId(), 1);
            }
            list.add(d);
        }
        if (CollUtils.isNotEmpty(list)) {
            recordDetailService.saveBatch(list);
        }
        // 3.实时累计每道题的作答次数和正确次数
        questionService.increaseAnswerStatistics(answerTimes, correctTimes);

        // 4.更新考试记录
        ExamRecord r = new ExamRecord();
        r.setId(recordId);
        r.setScore(score);
        r.setCorrectQuestions(correct);
        r.setDuration((int) DateUtils.between(record.getCreateTime(), LocalDateTime.now()).toSeconds());
        r.setFinishTime(LocalDateTime.now());
        r.setFinished(true);
        updateById(r);
        mqHelper.send(MqConstants.Exchange.LEARNING_EXCHANGE, MqConstants.Key.EXAM_FINISHED, userId);

    }

    @Override
    public List<ExamRecordDetailVO> queryDetailsByExamId(Long examId) {
        ExamRecord exam = getById(examId);
        if(exam == null || !Objects.equals(exam.getUserId(), UserContext.getUser())){
            throw new BadRequestException("考试记录不存在");
        }
        // 1.查询考试详情
        List<ExamRecordDetail> details = recordDetailService.lambdaQuery()
                .eq(ExamRecordDetail::getExamId, examId)
                .list();
        AssertUtils.isNotEmpty(details, "考试数据不存在");
        // 2.获取问题信息
        List<Long> qIds = details.stream().map(ExamRecordDetail::getQuestionId).collect(Collectors.toList());
        // 3.查询问题
        List<QuestionDTO> questions = questionService.queryQuestionByIds(qIds);
        AssertUtils.isTrue(questions.size() == qIds.size(), "问题不存在");
        Map<Long, QuestionDTO> qMap = questions.stream()
                .collect(Collectors.toMap(QuestionDTO::getId, Function.identity()));
        // 4.组织VO
        List<ExamRecordDetailVO> list = new ArrayList<>(details.size());
        for (ExamRecordDetail detail : details) {
            ExamRecordDetailVO v = BeanUtils.toBean(detail, ExamRecordDetailVO.class);
            v.setQuestion(qMap.get(detail.getQuestionId()));
            list.add(v);
        }
        return list;
    }


    private void checkStageExamAllowed(ExamFormDTO examFormDTO) {
        CourseFullInfoDTO courseInfo =
                courseClient.getCourseInfoById(examFormDTO.getCourseId(), true, false);
        if (courseInfo == null || CollUtils.isEmpty(courseInfo.getChapters())) {
            throw new BadRequestException("课程目录不存在");
        }
        CatalogueDTO examCatalogue = findCatalogue(courseInfo.getChapters(), examFormDTO.getSectionId());
        if (examCatalogue == null) {
            throw new BadRequestException("考试不存在");
        }
        CatalogueDTO chapter = findParentChapter(courseInfo.getChapters(), examFormDTO.getSectionId());
        if (chapter == null || CollUtils.isEmpty(chapter.getSections())) {
            throw new BadRequestException("考试章节不存在");
        }
        Set<Long> learnedSectionIds = queryLearnedSectionIds(examFormDTO.getCourseId());
        List<Long> sectionIds = chapter.getSections().stream()
                .filter(c -> c.getType() != null && c.getType() == 2)
                .map(CatalogueDTO::getId)
                .collect(Collectors.toList());
        if (CollUtils.isEmpty(sectionIds)) {
            throw new BadRequestException("考试章节未配置小节");
        }
        if (!learnedSectionIds.containsAll(sectionIds)) {
            throw new BadRequestException("学完本章所有小节后才能参加考试");
        }
    }

    private Set<Long> queryLearnedSectionIds(Long courseId) {
        LearningLessonDTO lesson = learningClient.queryLearningRecordByCourse(courseId);
        if (lesson == null || CollUtils.isEmpty(lesson.getRecords())) {
            return CollUtils.emptySet();
        }
        return lesson.getRecords().stream()
                .filter(r -> Boolean.TRUE.equals(r.getFinished()))
                .map(LearningRecordDTO::getSectionId)
                .collect(Collectors.toSet());
    }

    private CatalogueDTO findParentChapter(List<CatalogueDTO> chapters, Long catalogueId) {
        for (CatalogueDTO chapter : chapters) {
            if (Objects.equals(chapter.getId(), catalogueId)) {
                return chapter;
            }
            if (CollUtils.isEmpty(chapter.getSections())) {
                continue;
            }
            boolean matched = chapter.getSections().stream()
                    .anyMatch(s -> Objects.equals(s.getId(), catalogueId));
            if (matched) {
                return chapter;
            }
        }
        return null;
    }

    private CatalogueDTO findCatalogue(List<CatalogueDTO> catalogues, Long catalogueId) {
        if (CollUtils.isEmpty(catalogues)) {
            return null;
        }
        for (CatalogueDTO catalogue : catalogues) {
            if (Objects.equals(catalogue.getId(), catalogueId)) {
                return catalogue;
            }
            CatalogueDTO child = findCatalogue(catalogue.getSections(), catalogueId);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

}
