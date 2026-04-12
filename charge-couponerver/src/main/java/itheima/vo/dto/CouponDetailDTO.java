package itheima.vo.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;

@Data
public class CouponDetailDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int categoryId;
    private int scope; // 优惠券范围
    private int remainCount; // 优惠券剩余数量
    private Date expireDate; // 优惠券过期时间
}
