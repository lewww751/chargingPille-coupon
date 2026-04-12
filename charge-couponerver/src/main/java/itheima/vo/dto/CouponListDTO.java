package itheima.vo.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 优惠券列表DTO
 */
@Data
public class CouponListDTO implements Serializable {
    private static final long serialVersionUID = 1L;



    private String name;
    private Long categoryId;
    private Integer totalCount;
    private BigDecimal price;
    private int scope;
}
