package com.tianji.learning.service.impl;

import com.tianji.common.utils.BeanUtils;
import com.tianji.learning.domian.po.PointsBoardSeason;
import com.tianji.learning.domian.vo.PointsRecordSeasonVO;
import com.tianji.learning.mapper.PointsRecordSeasonMapper;
import com.tianji.learning.service.IPointsRecordSeasonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wyy
 */
@Service
public class PointsRecordSeasonServiceImpl extends ServiceImpl<PointsRecordSeasonMapper, PointsBoardSeason> implements IPointsRecordSeasonService {

    /**
     * 查询赛季列表
     * @return
     */
    @Override
    public List<PointsRecordSeasonVO> querySeasonList() {
        List<PointsBoardSeason> list = lambdaQuery()
                .orderByDesc(PointsBoardSeason::getBeginTime)
                .list();
        return BeanUtils.copyList(list, PointsRecordSeasonVO.class);
    }

    /**
     * 查询赛季
     * @param time
     * @return
     */
    @Override
    public Integer querySeasonByTime(LocalDateTime time) {
        Optional<PointsBoardSeason> optional = lambdaQuery()
                .le(PointsBoardSeason::getBeginTime, time)
                .ge(PointsBoardSeason::getEndTime, time)
                .oneOpt();
        return optional.map(PointsBoardSeason::getId).orElse(null);
    }
}
