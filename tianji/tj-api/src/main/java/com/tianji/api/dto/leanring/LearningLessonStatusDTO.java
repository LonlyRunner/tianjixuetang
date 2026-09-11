package com.tianji.api.dto.leanring;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ApiModel(description = "用户课表课程状态信息")
public class LearningLessonStatusDTO {
    @ApiModelProperty("主键lessonId")
    private Long id;
    @ApiModelProperty("课程id")
    private Long courseId;
    @ApiModelProperty("课程状态")
    private Integer status;
    @ApiModelProperty("已学习小节数量")
    private Integer learnedSections;
    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;
    @ApiModelProperty("过期时间")
    private LocalDateTime expireTime;
    @ApiModelProperty("学习计划状态")
    private Integer planStatus;
}