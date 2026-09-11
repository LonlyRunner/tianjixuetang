package com.tianji.learning.service;

import com.tianji.learning.domian.po.PointsBoard;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domian.po.PointsRecord;
import com.tianji.learning.domian.vo.PointsStatisticsVO;
import com.tianji.learning.enums.PointsRecordType;

import java.util.List;

/**
 * <p>
 * 学霸天梯榜 服务类
 * </p>
 *
 * @author wyy
 */
public interface IPointsRecordService extends IService<PointsRecord> {

    void addPointsRecord(Long userId, int points, PointsRecordType type);

    List<PointsStatisticsVO> queryMyPointsToday();
}
