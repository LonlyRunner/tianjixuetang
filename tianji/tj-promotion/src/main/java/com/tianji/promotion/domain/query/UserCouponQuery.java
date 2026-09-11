package com.tianji.promotion.domain.query;

import com.tianji.common.domain.query.PageQuery;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(description = "我的优惠券查询条件")
public class UserCouponQuery extends PageQuery {

    @ApiModelProperty("优惠券状态：1-未使用，2-已使用，3-已过期")
    private Integer status;
}