package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.tianji.learning.domian.po.PointsBoardSeason;
import com.tianji.learning.service.IPointsBoardSeasonService;
import org.springframework.stereotype.Service;
import com.tianji.learning.mapper.PointsBoardSeasonMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class PointsBoardSeasonServiceImpl extends ServiceImpl<PointsBoardSeasonMapper, PointsBoardSeason> implements IPointsBoardSeasonService {

    @Override
    public Integer querySeasonByTime(LocalDateTime time) {
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .oneOpt();
        return optional.map(PointsBoardSeason::getId).orElse(null);
    }


    @Override
    public List<PointsBoardSeason> querySeasonList() {
        return lambdaQuery()
                .orderByDesc(PointsBoardSeason::getBeginTime)
                .list();
    }
}