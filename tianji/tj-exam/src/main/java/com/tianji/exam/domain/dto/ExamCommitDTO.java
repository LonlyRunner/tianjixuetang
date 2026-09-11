package com.tianji.exam.domain.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Data
@ApiModel(description = "考试提交的答案信息")
public class ExamCommitDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    @ApiModelProperty("考试记录id，考试开始时")
    @NotNull(message = "考试记录id不能为空")
    private Long id;
    @Valid
    @NotEmpty(message = "答题信息不能为空")
    private List<ExamDetailDTO> examDetails;
}

