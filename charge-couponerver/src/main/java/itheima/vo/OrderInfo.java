package itheima.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class OrderInfo {
    private Long orderId;
    private Date orderTime;
    private BigDecimal orderPrice;
    private long userPhone;
    private String userName;
    private int categoryId;
}
