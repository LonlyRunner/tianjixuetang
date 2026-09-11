package com.tianji.learning.mq;

import com.tianji.api.dto.remark.LikedTimesDTO;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domian.po.InteractionReply;
import com.tianji.learning.domian.po.Note;
import com.tianji.learning.properties.LikeProperties;
import com.tianji.learning.service.IInteractionReplyService;
import com.tianji.learning.service.INoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static com.tianji.common.constants.MqConstants.Exchange.LIKE_RECORD_EXCHANGE;
import static com.tianji.common.constants.MqConstants.Key.LIKED_TIMES_KEY_TEMPLATE;

@Slf4j
@Component
@RequiredArgsConstructor
public class LikeTimesChangeListener {

    private final IInteractionReplyService replyService;
    private final INoteService noteService;
    private final LikeProperties likeProperties;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "qa.liked.times.queue", durable = "true"),
            exchange = @Exchange(name = LIKE_RECORD_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = "*.times.changed"
    ))
    public void listenReplyLikedTimesChange(
            List<LikedTimesDTO> likedTimesDTOs,
            @Header(name = "amqp_receivedRoutingKey", required = false) String routingKey) {

        String replyBizType = likeProperties.getReplyBizType();
        String expectedKey = StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, replyBizType);

        if (expectedKey.equals(routingKey)) {
            log.debug("收到回答/评论的点赞数变更");
            List<InteractionReply> list = new ArrayList<>(likedTimesDTOs.size());
            for (LikedTimesDTO dto : likedTimesDTOs) {
                InteractionReply r = new InteractionReply();
                r.setId(dto.getBizId());
                r.setLikedTimes(dto.getLikedTimes());
                list.add(r);
            }
            replyService.updateBatchById(list);
            return;
        }
        String noteKey = StringUtils.format(LIKED_TIMES_KEY_TEMPLATE, likeProperties.getNoteBizType());
        if (noteKey.equals(routingKey)) {
            log.debug("收到学习笔记的点赞数变更");
            List<Note> notes = new ArrayList<>(likedTimesDTOs.size());
            for (LikedTimesDTO dto : likedTimesDTOs) {
                notes.add(new Note().setId(dto.getBizId()).setLikedTimes(dto.getLikedTimes()));
            }
            noteService.updateBatchById(notes);
        }
    }
}
