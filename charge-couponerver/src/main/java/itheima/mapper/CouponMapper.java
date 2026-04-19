package itheima.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import itheima.vo.CouponVo;
import itheima.vo.CouponW;
import itheima.vo.coupon;
import itheima.vo.dto.CouponDetailDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CouponMapper extends BaseMapper<CouponW> {

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

    /**
     * 扣减库存(乐观锁--剩余量>0)
     */
    @Update("UPDATE t_coupon SET remain_count = remain_count - 1 WHERE category_id = #{categoryId} ")
    void decrStockCount(int categoryId);
    //(乐观锁--剩余量>0)
    @Update("UPDATE t_coupon SET remain_count = remain_count - 1 WHERE category_id = #{categoryId} "+
            "AND remain_count > 0")
    int decrStockCountv2(int category);

    /**
     * 根据用户手机号查询订单
     */
    @Select("SELECT 1 FROM t_order WHERE user_id = #{phone} ORDER BY create_time DESC LIMIT 1")
    boolean existsOrderByUserId(long phone);

    /**
     * 根据优惠券id查询库存
     */
    @Select("SELECT remain_count FROM t_coupon WHERE category_id = #{couponId}")
    Long selectStockById(Long couponId);

    /**
     * 修改订单状态
     * @param orderId
     * @param statusCancel
     * @param payTimeOnline
     * @return
     */
    @Update("UPDATE t_order " +
            "SET status_cancel = #{statusCancel}" +
            "WHERE order_id = #{orderId} AND status = 0")
    int changeOrderStatus(Long orderId, String statusCancel, String payTimeOnline);
    /**
     * 增加库存
     * @param couponId
     * */
    @Update("UPDATE t_coupon SET remain_count = remain_count + 1 WHERE category_id = #{couponId}")
    void incrStockCount(Long couponId);
}
