package com.tianji.learning.service;

import com.tianji.learning.domian.po.SignRecord;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.learning.domian.vo.SignResultVO;

/**
 * <p>
 * 学习积分记录，每个月底清零 服务类
 * </p>
 *
 * @author wyy
 */
public interface ISignRecordService extends IService<SignRecord> {


    SignResultVO addSignRecords();
}
