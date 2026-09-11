package com.tianji.promotion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.exceptions.BizIllegalException;
import com.tianji.common.utils.*;
import com.tianji.promotion.constants.PromotionConstants;
import com.tianji.promotion.domain.dto.CouponFormDTO;
import com.tianji.promotion.domain.dto.CouponIssueFormDTO;
import com.tianji.promotion.domain.po.Coupon;
import com.tianji.promotion.domain.po.CouponScope;
import com.tianji.promotion.domain.po.UserCoupon;
import com.tianji.promotion.domain.query.CouponQuery;
import com.tianji.promotion.domain.vo.CouponDetailVO;
import com.tianji.promotion.domain.vo.CouponPageVO;
import com.tianji.promotion.domain.vo.CouponScopeVO;
import com.tianji.promotion.domain.vo.CouponVO;
import com.tianji.promotion.enums.CouponStatus;
import com.tianji.promotion.enums.ObtainType;
import com.tianji.promotion.enums.UserCouponStatus;
import com.tianji.promotion.mapper.CouponMapper;
import com.tianji.promotion.service.ICouponScopeService;
import com.tianji.promotion.service.ICouponService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tianji.promotion.service.IExchangeCodeService;
import com.tianji.promotion.service.IUserCouponService;
import com.xxl.job.core.context.XxlJobHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tianji.promotion.enums.CouponStatus.*;

/**
 * <p>
 * 优惠券的规则信息 服务实现类
 * </p>
 *
 * @author wyy
 */
@Service
@RequiredArgsConstructor
public class CouponServiceImpl extends ServiceImpl<CouponMapper, Coupon> implements ICouponService {


    private final ICouponScopeService scopeService;

    private final IExchangeCodeService codeService;

    private final IUserCouponService  userCouponService;

    private final StringRedisTemplate redisTemplate;


    @Override
    @Transactional
    public void saveCoupon(CouponFormDTO dto) {
        // 1.保存优惠券
        // 1.1.转PO
        Coupon coupon = BeanUtils.copyBean(dto, Coupon.class);
        // 1.2.保存
        save(coupon);

        if (!dto.getSpecific()) {
            // 没有范围限定
            return;
        }
        Long couponId = coupon.getId();
        // 2.保存限定范围
        List<Long> scopes = dto.getScopes();
        if (CollUtils.isEmpty(scopes)) {
            throw new BadRequestException("限定范围不能为空");
        }
        // 2.1.转换PO
        List<CouponScope> list = scopes.stream()
                .map(bizId -> new CouponScope().setBizId(bizId).setCouponId(couponId))
                .collect(Collectors.toList());
        // 2.2.保存
        scopeService.saveBatch(list);
    }


    @Override
    public PageDTO<CouponPageVO> queryCouponByPage(CouponQuery query) {
        Integer status = query.getStatus();
        String name = query.getName();
        Integer type = query.getType();
        // 1.分页查询
        Page<Coupon> page = lambdaQuery()
                .eq(type != null, Coupon::getDiscountType, type)
                .eq(status != null, Coupon::getStatus, status)
                .like(StringUtils.isNotBlank(name), Coupon::getName, name)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        // 2.处理VO
        List<Coupon> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        List<CouponPageVO> list = BeanUtils.copyList(records, CouponPageVO.class);
        // 3.返回
        return PageDTO.of(page, list);
    }


    /**
     * 优惠券发放
     */
    @Transactional
    @Override
    public void beginIssue(CouponIssueFormDTO dto) {
        // 1.查询优惠券
        Coupon coupon = getById(dto.getId());
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在！");
        }
        // 2.判断优惠券状态，是否是暂停或待发放
        if(coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != PAUSE){
            throw new BizIllegalException("优惠券状态错误！");
        }
        // 3.判断是否是立刻发放
        LocalDateTime issueBeginTime = dto.getIssueBeginTime();
        LocalDateTime now = LocalDateTime.now();
        boolean isBegin = issueBeginTime == null || !issueBeginTime.isAfter(now);
        // 4.更新优惠券
        // 4.1.拷贝属性到PO
        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        // 4.2.更新状态
        if (isBegin) {
            c.setStatus(ISSUING);
            c.setIssueBeginTime(now);
        }else{
            c.setStatus(UN_ISSUE);
        }
        // 4.3.写入数据库
        updateById(c);

        // 5.添加缓存，前提是立刻发放的
        if (isBegin) {
            coupon.setIssueBeginTime(c.getIssueBeginTime());
            coupon.setIssueEndTime(c.getIssueEndTime());
            cacheCouponInfo(coupon);
        }

        // 5.判断是否需要生成兑换码，优惠券类型必须是兑换码，优惠券状态必须是待发放
        if(coupon.getObtainWay() == ObtainType.ISSUE && coupon.getStatus() == CouponStatus.DRAFT){
            coupon.setIssueEndTime(c.getIssueEndTime());
            codeService.asyncGenerateCode(coupon);
        }
    }
    /**
     * 缓存优惠券信息
     * @param coupon 优惠券
     */
    private void cacheCouponInfo(Coupon coupon) {
        // 1.组织数据
        Map<String, String> map = new HashMap<>(4);
        map.put("issueBeginTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueBeginTime())));
        map.put("issueEndTime", String.valueOf(DateUtils.toEpochMilli(coupon.getIssueEndTime())));
        map.put("totalNum", String.valueOf(coupon.getTotalNum()));
        map.put("userLimit", String.valueOf(coupon.getUserLimit()));
        // 2.写缓存
        redisTemplate.opsForHash().putAll(PromotionConstants.COUPON_CACHE_KEY_PREFIX + coupon.getId(), map);
    }


    @Override
    @Transactional
    public void updateCoupon(Long id, CouponFormDTO dto) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        if (coupon.getStatus() != CouponStatus.DRAFT && coupon.getStatus() != CouponStatus.PAUSE) {
            throw new BizIllegalException("只有草稿或暂停状态的优惠券可以修改");
        }

        Coupon c = BeanUtils.copyBean(dto, Coupon.class);
        c.setId(id);
        c.setStatus(null); // 不修改状态
        updateById(c);

        Boolean specific = dto.getSpecific();
        if (Boolean.TRUE.equals(specific)) {
            List<Long> scopes = dto.getScopes();
            if (CollUtils.isEmpty(scopes)) {
                throw new BadRequestException("限定范围不能为空");
            }
            // 删除旧范围，重新插入
            scopeService.remove(
                    new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, id));
            List<CouponScope> list = scopes.stream()
                    .map(bizId -> new CouponScope().setCouponId(id).setBizId(bizId))
                    .collect(Collectors.toList());
            scopeService.saveBatch(list);
        } else if (Boolean.FALSE.equals(specific)) {
            scopeService.remove(
                    new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, id));
        }
    }

    @Override
    @Transactional
    public void deleteCouponById(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        if (coupon.getStatus() != CouponStatus.DRAFT) {
            throw new BizIllegalException("只能删除草稿状态的优惠券");
        }
        removeById(id);
        scopeService.remove(
                new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, id));
    }

    @Override
    public CouponDetailVO queryCouponById(Long id) {
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }
        CouponDetailVO vo = BeanUtils.copyBean(coupon, CouponDetailVO.class);

        List<CouponScope> scopes = scopeService.list(
                new LambdaQueryWrapper<CouponScope>().eq(CouponScope::getCouponId, id));
        if (CollUtils.isNotEmpty(scopes)) {
            List<CouponScopeVO> scopeVOs = scopes.stream()
                    .map(s -> new CouponScopeVO(s.getBizId(), null))
                    .collect(Collectors.toList());
            vo.setScopes(scopeVOs);
        }
        return vo;
    }

    @Override
    @Transactional
    public void pauseIssue(Long id) {
        // 1.查询旧优惠券
        Coupon coupon = getById(id);
        if (coupon == null) {
            throw new BadRequestException("优惠券不存在");
        }

        // 2.当前券状态必须是未开始或进行中
        CouponStatus status = coupon.getStatus();
        if (status != UN_ISSUE && status != ISSUING) {
            // 状态错误，直接结束
            return;
        }

        // 3.更新状态
        boolean success = lambdaUpdate()
                .set(Coupon::getStatus, PAUSE)
                .eq(Coupon::getId, id)
                .in(Coupon::getStatus, UN_ISSUE, ISSUING)
                .update();
        if (!success) {
            // 可能是重复更新，结束
            log.error("重复暂停优惠券");
        }

        // 4.删除缓存
        redisTemplate.delete(PromotionConstants.COUPON_CACHE_KEY_PREFIX + id);
    }

    @Override
    public void checkIssueStart() {
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        LocalDateTime now = LocalDateTime.now();

        List<Coupon> list = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                .le(Coupon::getIssueBeginTime, now)
                .apply(total > 0, "id % {0} = {1}", total, index)
                .list();

        for (Coupon coupon : list) {
            lambdaUpdate()
                    .set(Coupon::getStatus, CouponStatus.ISSUING)
                    .set(Coupon::getUpdateTime, LocalDateTime.now())
                    .eq(Coupon::getId, coupon.getId())
                    .eq(Coupon::getStatus, CouponStatus.UN_ISSUE)
                    .update();
        }
    }

    @Override
    public void checkIssueEnd() {
        int index = XxlJobHelper.getShardIndex();
        int total = XxlJobHelper.getShardTotal();
        LocalDateTime now = LocalDateTime.now();

        List<Coupon> list = lambdaQuery()
                .eq(Coupon::getStatus, CouponStatus.ISSUING)
                .le(Coupon::getIssueEndTime, now)
                .apply(total > 0, "id % {0} = {1}", total, index)
                .list();

        for (Coupon coupon : list) {
            lambdaUpdate()
                    .set(Coupon::getStatus, CouponStatus.FINISHED)
                    .set(Coupon::getUpdateTime, LocalDateTime.now())
                    .eq(Coupon::getId, coupon.getId())
                    .eq(Coupon::getStatus, CouponStatus.ISSUING)
                    .update();
        }
    }


    @Override
    public List<CouponVO> queryIssuingCoupons() {
        // 1.查询发放中的优惠券列表
        List<Coupon> coupons = lambdaQuery()
                .eq(Coupon::getStatus, ISSUING)
                .eq(Coupon::getObtainWay, ObtainType.PUBLIC)
                .list();
        if (CollUtils.isEmpty(coupons)) {
            return CollUtils.emptyList();
        }
        // 2.统计当前用户已经领取的优惠券的信息
        List<Long> couponIds = coupons.stream().map(Coupon::getId).collect(Collectors.toList());
        // 2.1.查询当前用户已经领取的优惠券的数据
        List<UserCoupon> userCoupons = userCouponService.lambdaQuery()
                .eq(UserCoupon::getUserId, UserContext.getUser())
                .in(UserCoupon::getCouponId, couponIds)
                .list();
        // 2.2.统计当前用户对优惠券的已经领取数量
        Map<Long, Long> issuedMap = userCoupons.stream()
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 2.3.统计当前用户对优惠券的已经领取并且未使用的数量
        Map<Long, Long> unusedMap = userCoupons.stream()
                .filter(uc -> uc.getStatus() == UserCouponStatus.UNUSED)
                .collect(Collectors.groupingBy(UserCoupon::getCouponId, Collectors.counting()));
        // 3.封装VO结果
        List<CouponVO> list = new ArrayList<>(coupons.size());
        for (Coupon c : coupons) {
            // 3.1.拷贝PO属性到VO
            CouponVO vo = BeanUtils.copyBean(c, CouponVO.class);
            list.add(vo);
            // 3.2.是否可以领取：已经被领取的数量 < 优惠券总数量 && 当前用户已经领取的数量 < 每人限领数量
            vo.setAvailable(
                    c.getIssueNum() < c.getTotalNum()
                            && issuedMap.getOrDefault(c.getId(), 0L) < c.getUserLimit()
            );
            // 3.3.是否可以使用：当前用户已经领取并且未使用的优惠券数量 > 0
            vo.setReceived(unusedMap.getOrDefault(c.getId(),  0L) > 0);
        }
        return list;
    }
}
