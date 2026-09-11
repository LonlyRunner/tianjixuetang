package com.tianji.promotion.handler;

import com.tianji.promotion.service.IUserCouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserCouponRemindJobHandler {

    private final IUserCouponService userCouponService;

    @XxlJob("couponExpireRemindJobHandler")
    public void remindExpireCoupon() {
        log.info("优惠券过期提醒任务开始");
        userCouponService.remindExpireCoupon();
        log.info("优惠券过期提醒任务结束");
    }
}