package itheima.mq.listener;

import itheima.mq.MQConstant;
import itheima.service.impl.CouponServiceImpl;
import itheima.vo.OrderMessage;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 超时订单检查
 */
@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_PAY_TIMEOUT_CONSUMER_GROUP,
        topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC
)
public class OrderTimeOutCheckMessageListener implements RocketMQListener<OrderMessage> {
    @Autowired
    private CouponServiceImpl couponService;
    @Override
    public void onMessage(OrderMessage orderMessage) {
        couponService.checkOrderTimeout(orderMessage);
    }
}
