package itheima.vo;

import lombok.*;

@Getter
@Setter

@NoArgsConstructor
@AllArgsConstructor
public class OrderMessage {
    private Long couponId;
    private Long orderId;
    private String token;
    private Long userPhone;
}
