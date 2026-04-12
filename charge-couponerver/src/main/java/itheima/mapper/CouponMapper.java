package itheima.mapper;

import itheima.vo.CouponVo;
import itheima.vo.dto.CouponDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CouponMapper {

    /**
     * 查询所有启用的优惠券
     */
    @Select("SELECT category_id, name, total_count, price, scope " +
            "FROM t_coupon WHERE status = 1 ORDER BY price DESC")
    List<CouponVo> selectAllEnabled();

    /**
     * 查询热点优惠券
     */
    @Select("SELECT category_id, name, total_count, price, scope " +
            "FROM t_coupon WHERE status = 1 AND is_hot = 1 ORDER BY price DESC")
    List<CouponVo> selectHotCoupons();
    /**
     * 优惠券详情查询
     */
    @Select("SELECT category_id, scope, remain_count, expire_date " +
            "FROM t_coupon WHERE category_id = #{categoryId}")
    CouponDetailDTO selectByCategoryId(Integer categoryId);
}
