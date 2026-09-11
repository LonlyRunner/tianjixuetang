package com.tianji.api.dto.leanring;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel(description = "学习记录综合信息")
public class LearningLessonDTO {
    @ApiModelProperty("主键lessonId")
    private Long id;
    @ApiModelProperty("最新学习的小节id")
    private Long latestSectionId;
    @ApiModelProperty("学习记录")
    private List<LearningRecordDTO> records;
}