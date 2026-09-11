package com.tianji.learning.mq;

import com.tianji.common.constants.MqConstants;
import com.tianji.learning.enums.PointsRecordType;
import com.tianji.learning.mq.message.PointsRecordMessage;
import com.tianji.learning.mq.message.SignInMessage;
import com.tianji.learning.service.IPointsRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningPointsListener {

    private final IPointsRecordService recordService;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "reply.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_REPLY
    ))
    public void listenReplyMessage(PointsRecordMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        recordService.addPointsRecord(
                message.getUserId(),
                message.getPoints(),
                PointsRecordType.of(message.getType()));
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "learn.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.LEARN_SECTION
    ))
    public void listenLearnSectionMessage(PointsRecordMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        recordService.addPointsRecord(
                message.getUserId(),
                message.getPoints(),
                PointsRecordType.of(message.getType()));
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "note.new.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.WRITE_NOTE
    ))
    public void listenWriteNoteMessage(Long userId) {
        if (userId == null) {
            return;
        }
        recordService.addPointsRecord(userId, PointsRecordType.NOTE.getMaxPoints(), PointsRecordType.NOTE);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "note.gathered.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.NOTE_GATHERED
    ))
    public void listenNoteGatheredMessage(Long userId) {
        if (userId == null) {
            return;
        }
        recordService.addPointsRecord(userId, PointsRecordType.NOTE.getMaxPoints(), PointsRecordType.NOTE);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "exam.finished.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.EXAM_FINISHED
    ))
    public void listenExamFinishedMessage(Long userId) {
        if (userId == null) {
            return;
        }
        recordService.addPointsRecord(userId, PointsRecordType.EXAM.getMaxPoints(), PointsRecordType.EXAM);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "sign.points.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.LEARNING_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.SIGN_IN
    ))
    public void listenSignInMessage(SignInMessage message) {
        if (message == null || message.getUserId() == null) {
            return;
        }
        recordService.addPointsRecord(
                message.getUserId(),
                message.getPoints(),
                PointsRecordType.SIGN);
    }
}
