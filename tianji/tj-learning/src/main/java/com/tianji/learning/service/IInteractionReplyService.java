package com.tianji.learning.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domian.dto.ReplyFormDTO;
import com.tianji.learning.domian.po.InteractionReply;
import com.tianji.learning.domian.query.ReplyPageQuery;
import com.tianji.learning.domian.vo.ReplyVO;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IInteractionReplyService extends IService<InteractionReply> {

    /**
     * 新增回答或评论
     */
    void saveReply(ReplyFormDTO replyDTO);

    /**
     * 分页查询回答或评论
     */
    PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query);

    /**
     * 管理端分页查询回答或评论
     */
    PageDTO<ReplyVO> queryReplyPageForAdmin(ReplyPageQuery query);

    /**
     * 管理端隐藏或显示回答/评论
     */
    void hiddenReply(Long id, Boolean hidden);
}