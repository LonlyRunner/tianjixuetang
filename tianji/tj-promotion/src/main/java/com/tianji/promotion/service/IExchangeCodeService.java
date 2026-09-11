package com.tianji.promotion.service;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.ExchangeCode;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.CodeVO;

/**
 * <p>
 * 兑换码 服务类
 * </p>
 *
 * @author wyy
 */
public interface IExchangeCodeService extends IService<ExchangeCode> {


    void asyncGenerateCode(Coupon coupon);

    PageDTO<CodeVO> queryCodePage(CodeQuery query);

    boolean updateExchangeMark(long serialNum, boolean mark);

    Long exchangeTargetId(long serialNum);
}
