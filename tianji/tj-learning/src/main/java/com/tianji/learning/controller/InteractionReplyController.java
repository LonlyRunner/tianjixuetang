package com.tianji.learning.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.learning.domian.dto.ReplyFormDTO;
import com.tianji.learning.domian.query.ReplyPageQuery;
import com.tianji.learning.domian.vo.ReplyVO;
import com.tianji.learning.service.IInteractionReplyService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Api(tags = "评论/回答相关接口")
@RestController
@RequiredArgsConstructor
@RequestMapping("/replies")
public class InteractionReplyController {

    private final IInteractionReplyService replyService;

    @ApiOperation("新增回答或评论")
    @PostMapping
    public void saveReply(@RequestBody ReplyFormDTO replyDTO) {
        replyService.saveReply(replyDTO);
    }

    @ApiOperation("分页查询回答或评论")
    @GetMapping("/page")
    public PageDTO<ReplyVO> queryReplyPage(ReplyPageQuery query) {
        return replyService.queryReplyPage(query);
    }

    @ApiOperation("管理端分页查询回答或评论")
    @GetMapping("/admin/page")
    public PageDTO<ReplyVO> queryReplyPageForAdmin(ReplyPageQuery query) {
        return replyService.queryReplyPageForAdmin(query);
    }

    @ApiOperation("管理端隐藏或显示回答/评论")
    @PutMapping("/admin/{id}/hidden/{hidden}")
    public void hiddenReply(
            @PathVariable Long id,
            @PathVariable Boolean hidden) {
        replyService.hiddenReply(id, hidden);
    }
}