package itheima.constant;

public interface MQConstant {
    // ── 优惠券相关 ──
    String TOPIC_COUPON         = "charge-coupon-topic";
    String TAG_COUPON_CLAIM     = "claim";        // 领取券
    String TAG_COUPON_EXPIRE    = "expire";       // 券过期
    String TAG_STOCK_DEDUCT     = "stock-deduct"; // 库存扣减

    // ── 订单相关 ──
    String TOPIC_ORDER          = "charge-order-topic";
    String TAG_ORDER_CREATE     = "create";       // 订单创建
    String TAG_ORDER_PAY        = "pay";          // 订单支付
    String TAG_ORDER_CANCEL     = "cancel";       // 订单取消
    String TAG_ORDER_REFUND     = "refund";       // 订单退款

    // ── 延迟消息（订单超时取消）──
    String TOPIC_ORDER_DELAY    = "charge-order-delay-topic";
    int    ORDER_TIMEOUT_MINUTE = 30;             // 订单超时分钟数

    // ── 消费者组 ──
    String GROUP_COUPON = "charge-coupon-consumer-group";
    String GROUP_ORDER  = "charge-order-consumer-group";
}
