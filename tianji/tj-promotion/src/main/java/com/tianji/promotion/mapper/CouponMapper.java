package com.tianji.promotion.mapper;

import com.tianji.promotion.domain.po.Coupon;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * <p>
 * 优惠券的规则信息 Mapper 接口
 * </p>
 *
 * @author wyy
 */
public interface CouponMapper extends BaseMapper<Coupon> {

    /**
     * 增加优惠券发放数量
     * @param couponId 优惠券id
     * @return 影响行数
     */
    @Update("UPDATE coupon SET issue_num = issue_num + 1 WHERE id = #{couponId} AND issue_num < total_num" )
    int incrIssueNum(@Param("couponId") Long couponId);

    /**
     * 增加优惠券已使用数量
     * @param ids 优惠券id列表
     * @param num 增加数量
     * @return 影响行数
     */
    @Update("UPDATE coupon SET used_num = used_num + #{num} WHERE id IN (${ids})")
    int incrUsedNum(@Param("ids") String ids, @Param("num") int num);
}
