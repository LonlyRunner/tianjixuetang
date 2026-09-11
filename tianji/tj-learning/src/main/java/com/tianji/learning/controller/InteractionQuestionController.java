package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domian.dto.QuestionFormDTO;
import com.tianji.learning.domian.query.QuestionPageQuery;
import com.tianji.learning.domian.vo.QuestionVO;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 控制器
 * </p>
 *
 * @author wyy
 */
@Api(tags = "InteractionQuestion管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/question")
public class InteractionQuestionController {


    private final IInteractionQuestionService questionService;


    /**
     * 新增提问
     */
    @ApiOperation("新增提问")
    @PostMapping
    public void saveQuestion(@Valid @RequestBody QuestionFormDTO questionDTO){
        questionService.saveQuestion(questionDTO);
    }

    /**
     * 分页查询互动问题
     */
    @ApiOperation("分页查询互动问题")
    @GetMapping("page")
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query){
        return questionService.queryQuestionPage(query);
    }


    /**
     * 根据id查询问题详情
     */
    @ApiOperation("根据id查询问题详情")
    @GetMapping("/{id}")
    public QuestionVO queryQuestionById(@ApiParam(value = "问题id", example = "1") @PathVariable("id") Long id){
        return questionService.queryQuestionById(id);
    }
    /**
     * 删除问题
     */
    @ApiOperation("删除问题")
    @DeleteMapping("/{id}")
    public void deleteQuestion(@PathVariable("id") Long id) {
        questionService.deleteQuestion(id);
    }
}
