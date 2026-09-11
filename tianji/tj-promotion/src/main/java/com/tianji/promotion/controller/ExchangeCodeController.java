package com.tianji.promotion.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.query.CodeQuery;
import com.tianji.promotion.domain.vo.CodeVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.domain.po.ExchangeCode;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 * 兑换码 控制器
 * </p>
 *
 * @author wyy
 */
@Api(tags = "ExchangeCode管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/exchangeCode")
public class ExchangeCodeController {

    private final IExchangeCodeService exchangeCodeService;


    @ApiOperation("查询兑换码分页接口")
    @GetMapping("/page")
    public PageDTO<CodeVO> queryCodePage(CodeQuery query) {
        return exchangeCodeService.queryCodePage(query);
    }

}
