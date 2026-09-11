package com.tianji.promotion.handler;

import com.tianji.promotion.service.ICouponService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CouponJobHandler {

    private final ICouponService couponService;

    @XxlJob("couponIssueStartJobHandler")
    public void issueStart() {
        log.info("优惠券开始发放任务执行");
        couponService.checkIssueStart();
    }

    @XxlJob("couponIssueEndJobHandler")
    public void issueEnd() {
        log.info("优惠券结束发放任务执行");
        couponService.checkIssueEnd();
    }
}