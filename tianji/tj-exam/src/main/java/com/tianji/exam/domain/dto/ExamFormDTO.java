package com.tianji.exam.domain.dto;


import com.tianji.exam.enums.ExamType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "考试记录表单信息")
public class ExamFormDTO {
    @ApiModelProperty("课程id")
    @NotNull(message = "课程id不能为空")
    private Long courseId;
    @ApiModelProperty("节id")
    @NotNull(message = "章节id不能为空")
    private Long sectionId;
    @ApiModelProperty("类型，1-练习，2-考试")
    @NotNull(message = "考试类型不能为空")
    private ExamType type;
}

