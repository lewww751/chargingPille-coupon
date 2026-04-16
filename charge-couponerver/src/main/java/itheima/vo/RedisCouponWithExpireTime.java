package itheima.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 带逻辑过期时间的coupon信息
 */
@Data
public class RedisCouponWithExpireTime {
    /**
     * 逻辑过期时间
     */
    private LocalDateTime expireTime;
    /**
     * 优惠券信息（真实数据）
     */
    private Object data;
}
