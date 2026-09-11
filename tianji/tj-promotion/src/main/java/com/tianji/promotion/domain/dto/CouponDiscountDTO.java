package com.tianji.promotion.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ApiModel(description = "订单的可用优惠券及折扣信息")
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CouponDiscountDTO {
    @ApiModelProperty("用户优惠券id集合")
    private List<Long> ids = new ArrayList<>();
    @ApiModelProperty("优惠券规则")
    private List<String> rules = new ArrayList<>();
    @ApiModelProperty("本订单最大优惠金额")
    private Integer discountAmount = 0;

    @ApiModelProperty("优惠券id")
    private Long couponId;
    @ApiModelProperty("优惠券名称")
    private String couponName;
    @ApiModelProperty("优惠金额")
    private Integer payAmount;
    @ApiModelProperty("课程id集合")
    private List<Long> courseIds;
    @ApiModelProperty("课程id-折扣明细")
    private Map<Long, Integer> discountDetail;
}