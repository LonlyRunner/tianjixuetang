package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domian.dto.QuestionFormDTO;
import com.tianji.learning.domian.po.InteractionQuestion;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domian.query.QuestionAdminPageQuery;
import com.tianji.learning.domian.query.QuestionPageQuery;
import com.tianji.learning.domian.vo.QuestionAdminVO;
import com.tianji.learning.domian.vo.QuestionVO;

/**
 * <p>
 * 互动提问的问题表 服务类
 * </p>
 *
 * @author wyy
 */
public interface IInteractionQuestionService extends IService<InteractionQuestion> {

    /**
     * 保存互动问题
     * @param questionDTO 问题表单信息
     */
    void saveQuestion(QuestionFormDTO questionDTO);

    /**
     * 分页查询问题
     * @param query 查询条件
     * @return 问题列表
     */
    PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query);


    /**
     * 根据id查询问题
     * @param id 问题id
     * @return 问题信息
     */
    QuestionVO queryQuestionById(Long id);


    /**
     * 删除问题（同时删除回答和评论）
     * @param id 问题id
     */
    void deleteQuestion(Long id);

    /**
     * 管理端分页查询问题
     * @param query 查询条件
     * @return 问题列表
     */
    PageDTO<QuestionAdminVO> queryQuestionPageAdmin(QuestionAdminPageQuery query);


    /**
     * 管理端根据id查询问题详情
     * @param id 问题id
     * @return 问题详情
     */
    QuestionAdminVO queryQuestionDetailForAdmin(Long id);


}
