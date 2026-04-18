package itheima.vo;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class OrderMQResult {
    private Long orderId;
    private Long couponId;
    private String msg;
    private Integer code;
    private Integer token;
}
