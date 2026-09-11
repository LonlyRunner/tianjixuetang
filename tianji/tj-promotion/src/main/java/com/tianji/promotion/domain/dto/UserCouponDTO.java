package com.tianji.promotion.domain.dto;

import lombok.Data;

@Data
public class UserCouponDTO {
    /**
     * 用户id
     */
    private Long userId;
    /**
     * 优惠券id
     */
    private Long couponId;
    /**
     * 兑换码序列号（兑换时使用）
     */
    private Integer serialNum;  // 添加这个字段
}