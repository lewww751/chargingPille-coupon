package itheima.result;

import lombok.Getter;

@Getter
public enum ResutCode {

    SUCCESS(200,    "操作成功"),
    FAIL(666,       "操作失败"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403,    "无权限访问"),
    NOT_FOUND(404,    "资源不存在"),
    PARAM_ERROR(400,  "参数校验失败"),
    SERVER_ERROR(500, "服务器内部错误"),

    // 业务码
    COUPON_NOT_FOUND(1001,  "优惠券不存在"),
    COUPON_SOLD_OUT(1002,   "优惠券已抢完"),
    COUPON_EXPIRED(1003,    "优惠券已过期"),
    COUPON_CLAIMED(1004,    "您已领取过该优惠券"),
    ORDER_NOT_FOUND(2001,   "订单不存在"),
    ORDER_PAY_FAIL(2002,    "支付失败"),
    USER_NOT_FOUND(3001,    "用户不存在"),
    STOCK_DEDUCT_FAIL(4001, "库存扣减失败");

    private final int    code;
    private final String msg;

    ResutCode(int code, String msg) {
        this.code = code;
        this.msg  = msg;
    }
}
