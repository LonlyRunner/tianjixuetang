package com.tianji.promotion.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("兑换码分页VO")
public class CodeVO {

    @ApiModelProperty("兑换码id")
    private Integer id;

    @ApiModelProperty("兑换码")
    private String code;
}