package itheima.common;

public interface ConpouConstant {
    // Redis Key
    String REDIS_STOCK_PREFIX = "coupon:stock:";
    String REDIS_USER_LIMIT_PREFIX = "coupon:user:limit:";

    // RocketMQ Topic & Tag
    String TOPIC_CREATE_ORDER = "TOPIC_CREATE_ORDER";
    String TAG_CREATE_ORDER = "TAG_CREATE_ORDER";

    String TOPIC_TIMEOUT_ORDER = "TOPIC_TIMEOUT_ORDER";
    String TAG_TIMEOUT_ORDER = "TAG_TIMEOUT_ORDER";

    // 订单状态
    int ORDER_STATUS_WAIT_PAY = 0;   // 待支付
    int ORDER_STATUS_PAID = 1;        // 已支付
    int ORDER_STATUS_CANCELED = 2;    // 已取消

    // 订单超时时间（秒）
    long ORDER_TIMEOUT_SECONDS = 30 * 60; // 30分钟

    // 每人限购数量
    int USER_LIMIT_COUNT = 1;
}
