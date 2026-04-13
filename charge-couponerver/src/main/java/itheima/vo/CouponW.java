package itheima.vo;

import lombok.Data;
import net.sf.jsqlparser.expression.DateTimeLiteralExpression;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 与数据库对应的优惠券实体类
 */
@Data
public class CouponW implements java.io.Serializable {

    private static final long serialVersionUID = 1L;


    private Integer id;
    private Integer categoryId;
    private String name;
    private long totalCount;
    private long remainCount;
    private Date startDate;
    private Date expireDate;
    private BigDecimal price;
    private int scope;
    private int status;
    private Date createTime;
    private Date updateTime;
    private int isHot;
}
