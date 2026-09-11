package com.tianji.promotion.constants;

public class PromotionConstants {

    /**
     * 兑换码序列号 Redis Key
     */
    public static final String COUPON_CODE_SERIAL_KEY = "promotion:code:serial";

    /**
     * 兑换码范围 Redis Key
     */
    public static final String COUPON_RANGE_KEY = "promotion:code:range";

    /**
     * 优惠券缓存 Redis Key 前缀
     */
    public static final String COUPON_CACHE_KEY_PREFIX = "promotion:coupon:";

    /**
     * 用户优惠券缓存 Redis Key 前缀
     */
    public static final String USER_COUPON_CACHE_KEY_PREFIX = "promotion:user:coupon:";

    // 兑换码使用状态位图，与序列号计数器分开
    public static final String COUPON_CODE_USED_KEY = "promotion:code:used";
}