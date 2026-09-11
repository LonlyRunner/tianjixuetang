package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangeListener {

    private final ILearningLessonService lessonService;

    /**
     * 监听支付成功，添加课程到课表
     * @param order 订单信息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.pay.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void listenLessonPay(OrderBasicDTO order){
        // 1.检查消息是否合法
        if(order == null || order.getUserId() == null || CollUtils.isEmpty(order.getCourseIds())){
            // 消息不合法，结束
            log.error("收到MQ消息不合法，缺少必要参数");
            return;
        }
        // 2.添加课程
        log.debug("处理用户{}的订单{}，需要添加课程{}到课表", order.getUserId(), order.getOrderId(), order.getCourseIds());
        lessonService.addUserLessons(order.getUserId(), order.getCourseIds());
    }

    /**
     * 监听退款成功，删除课表中的课程
     * @param order 订单信息
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "learning.lesson.refund.queue", durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void listenLessonRefund(OrderBasicDTO order){
        // 1.检查消息是否合法
        if(order == null || order.getUserId() == null || CollUtils.isEmpty(order.getCourseIds())){
            // 消息不合法，结束
            log.error("收到退款MQ消息不合法，缺少必要参数");
            return;
        }
        // 2.删除课表中的课程
        log.debug("处理用户{}的订单{}，需要从课表删除课程{}" , order.getUserId(), order.getOrderId(), order.getCourseIds());
        lessonService.deleteUserLessons(order.getUserId(), order.getCourseIds());
    }
}
