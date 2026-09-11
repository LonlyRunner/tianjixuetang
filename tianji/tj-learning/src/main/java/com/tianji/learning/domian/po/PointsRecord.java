package com.tianji.learning.domian.po;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.tianji.learning.enums.PointsRecordType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("points_record")
@ApiModel(value = "PointsRecord对象", description = "积分记录")
public class PointsRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "积分记录id")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "学员id")
    private Long userId;

    @ApiModelProperty(value = "积分值")
    private Integer points;

    @ApiModelProperty(value = "积分类型")
    private PointsRecordType type;

    @ApiModelProperty(value = "创建时间")
    private LocalDateTime createTime;
}