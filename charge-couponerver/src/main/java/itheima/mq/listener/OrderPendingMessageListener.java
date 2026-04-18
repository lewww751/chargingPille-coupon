package itheima.mq.listener;

import itheima.mq.DefaultCallBack;
import itheima.mq.MQConstant;
import itheima.service.impl.CouponServiceImpl;
import itheima.vo.OrderMQResult;
import itheima.vo.OrderMessage;
import jakarta.annotation.Resource;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;

@RocketMQMessageListener(
        consumerGroup = MQConstant.ORDER_PENDING_CONSUMER,
        topic = MQConstant.ORDER_PENDING_TOPIC
)
public class OrderPendingMessageListener implements RocketMQListener<OrderMessage> {
    @Resource
    private CouponServiceImpl couponService;
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(OrderMessage orderMessage) {
        OrderMQResult orderMQResult = new OrderMQResult();
        orderMessage.setToken(orderMessage.getToken());
        try {
            Long orderId = couponService.decrStockCountv2(orderMessage.getCouponId().intValue());
            orderMQResult.setOrderId(orderId);
            orderMQResult.setCode(200);
            orderMQResult.setMsg("订单创建成功");
        } catch (Exception e) {
            //订单创建失败
            orderMQResult.setCode(400);
            orderMQResult.setMsg("订单创建失败");
        }finally {
            rocketMQTemplate.asyncSend(MQConstant.ORDER_RESULT_TOPIC, orderMQResult, new DefaultCallBack("发送订单结果MQ"));
        }

    }
}
