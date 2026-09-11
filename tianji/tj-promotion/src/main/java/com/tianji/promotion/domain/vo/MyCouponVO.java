package com.tianji.promotion.domain.vo;

import com.tianji.promotion.enums.DiscountType;
import com.tianji.promotion.enums.UserCouponStatus;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "我的优惠券VO")
public class MyCouponVO {

    @ApiModelProperty("用户券id")
    private Long id;

    @ApiModelProperty("优惠券id")
    private Long couponId;

    @ApiModelProperty("优惠券名称")
    private String name;

    @ApiModelProperty("折扣类型")
    private DiscountType discountType;

    @ApiModelProperty("使用门槛")
    private Integer thresholdAmount;

    @ApiModelProperty("折扣值")
    private Integer discountValue;

    @ApiModelProperty("最高优惠金额")
    private Integer maxDiscountAmount;

    @ApiModelProperty("有效期开始时间")
    private LocalDateTime termBeginTime;

    @ApiModelProperty("有效期结束时间")
    private LocalDateTime termEndTime;

    @ApiModelProperty("状态")
    private UserCouponStatus status;
}