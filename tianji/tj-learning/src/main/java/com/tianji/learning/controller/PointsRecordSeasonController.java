package com.tianji.learning.controller;

import com.tianji.learning.domian.vo.PointsRecordSeasonVO;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.learning.service.IPointsRecordSeasonService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * <p>
 *  控制器
 * </p>
 *
 * @author wyy
 */
@Api(tags = "PointsBoardSeason管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/pointsBoardSeason")
public class PointsRecordSeasonController {

    private final IPointsRecordSeasonService seasonService;

    @ApiOperation("查询历史赛季列表")
    @GetMapping("/seasons/list")
    public List<PointsRecordSeasonVO> querySeasonList() {
        return seasonService.querySeasonList();
    }
}
