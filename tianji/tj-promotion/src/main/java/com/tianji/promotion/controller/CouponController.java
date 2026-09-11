package com.tianji.promotion.controller;

import com.tianji.common.domain.dto.PageDTO;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CodeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.service.ICouponService;
import com.tianji.promotion.service.IExchangeCodeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Api(tags = "优惠券管理")
@RestController
@RequiredArgsConstructor
@RequestMapping("/coupons")   // 与需求保持一致
public class CouponController {

    private final ICouponService couponService;
    private final IExchangeCodeService exchangeCodeService;

    @ApiOperation("新增优惠券接口")
    @PostMapping
    public void saveCoupon(@RequestBody @Valid CouponFormDTO dto){
        couponService.saveCoupon(dto);
    }

    @ApiOperation("分页查询优惠券接口")
    @GetMapping("/page")
    public PageDTO<com.tianji.promotion.domain.vo.CouponPageVO> queryCouponByPage(CouponQuery query){
        return couponService.queryCouponByPage(query);
    }

    @ApiOperation("开始发放优惠券接口")
    @PutMapping("/{id}/issue")
    public void beginIssue(@RequestBody @Valid CouponIssueFormDTO dto) {
        couponService.beginIssue(dto);
    }

    @ApiOperation("修改优惠券接口")
    @PutMapping("/{id}")
    public void updateCoupon(@PathVariable("id") Long id,
                             @RequestBody @Valid CouponFormDTO dto){
        couponService.updateCoupon(id, dto);
    }

    @ApiOperation("删除优惠券接口")
    @DeleteMapping("/{id}")
    public void deleteCoupon(@PathVariable("id") Long id){
        couponService.deleteCouponById(id);
    }

    @ApiOperation("根据id查询优惠券接口")
    @GetMapping("/{id}")
    public CouponDetailVO queryCouponById(@PathVariable("id") Long id){
        return couponService.queryCouponById(id);
    }

    @ApiOperation("暂停发放优惠券接口")
    @PutMapping("/{id}/pause")
    public void pauseIssue(@PathVariable("id") Long id){
        couponService.pauseIssue(id);
    }

    @ApiOperation("查询兑换码分页接口")
    @GetMapping("/codes/page")
    public PageDTO<CodeVO> queryCodePage(com.tianji.promotion.domain.query.CodeQuery query){
        return exchangeCodeService.queryCodePage(query);
    }

    @ApiOperation("查询发放中的优惠券列表")
    @GetMapping("/list")
    public List<CouponVO> queryIssuingCoupons(){
        return couponService.queryIssuingCoupons();
    }
}