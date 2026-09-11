package com.tianji.learning.controller;

import com.tianji.learning.domian.vo.SignResultVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.ISignRecordService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

/**
 * <p>
 * 学习积分记录，每个月底清零 控制器
 * </p>
 *
 * @author wyy
 */
@Api(tags = "PointsRecord管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pointsRecord")
public class SignRecordController {

    private final ISignRecordService recordService;

    @PostMapping
    @ApiOperation("签到功能接口")
    public SignResultVO addSignRecords(){
        return recordService.addSignRecords();
    }


}
