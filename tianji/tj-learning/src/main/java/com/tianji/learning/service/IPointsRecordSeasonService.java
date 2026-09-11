package com.tianji.learning.service;

import com.tianji.learning.domian.po.PointsBoardSeason;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domian.vo.PointsRecordSeasonVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author wyy
 */
public interface IPointsRecordSeasonService extends IService<PointsBoardSeason> {
    /**
     * 查询赛季列表
     * @return
     */
    List<PointsRecordSeasonVO> querySeasonList();


    /**
     * 根据时间查询赛季id
     * @param time 时间
     * @return 赛季id
     */
    Integer querySeasonByTime(LocalDateTime time);
}
