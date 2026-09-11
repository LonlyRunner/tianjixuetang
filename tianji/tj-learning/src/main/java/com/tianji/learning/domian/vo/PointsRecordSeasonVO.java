package com.tianji.learning.domian.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
@ApiModel(description = "赛季信息")
public class PointsRecordSeasonVO {
    @ApiModelProperty("赛季id")
    private Integer id;
    @ApiModelProperty("赛季名称")
    private String name;
    @ApiModelProperty("赛季开始时间")
    private LocalDate beginTime;
    @ApiModelProperty("赛季结束时间")
    private LocalDate endTime;
}