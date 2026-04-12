package itheima.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
@Data
public class CouponVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 总库存
     */
    private Integer totalCount;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 使用范围
     */
    private int scope;

//    /**
//     * 是否热点数据 (1-是 0-否)
//     */
//    private Integer isHot;

    /**
     * 状态 (1-启用 0-禁用)
     */
    private Integer status;
}
