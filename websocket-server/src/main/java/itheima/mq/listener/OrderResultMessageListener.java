package itheima.mq.listener;


import com.alibaba.fastjson.JSON;


import javax.websocket.Session;
import itheima.core.WebsocketServer;
import itheima.vo.OrderMQResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@RocketMQMessageListener(
        consumerGroup = "order_result_consumer_group",
        topic = "order_result_topic"
)
@Component
@Slf4j
public class OrderResultMessageListener implements RocketMQListener<OrderMQResult> {


    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        String jsonMQResult = JSON.toJSONString(orderMQResult);
        try {
            int count = 0;
            do {
                Session session = WebsocketServer.SESSION_MAP.get(orderMQResult.getToken());
                if (session != null){
                    session.getBasicRemote().sendText(jsonMQResult);
                    return;
                }
                count++;
                Thread.sleep(300);
            } while (count<=5);
        } catch (IOException e) {
            log.error("WebSocket connection error{}", e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
