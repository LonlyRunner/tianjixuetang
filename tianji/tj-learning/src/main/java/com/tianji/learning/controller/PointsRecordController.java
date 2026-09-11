package com.tianji.learning.controller;

import com.tianji.learning.domian.vo.PointsStatisticsVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.IPointsRecordService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 控制器
 * </p>
 *
 * @author wyy
 */
@Api(tags = "PointsBoard管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pointsBoard")
public class PointsRecordController {

    private final IPointsRecordService pointsRecordService;

    @ApiOperation("查询我的今日积分")
    @GetMapping("today")
    public List<PointsStatisticsVO> queryMyPointsToday(){
        return pointsRecordService.queryMyPointsToday();
    }
}
