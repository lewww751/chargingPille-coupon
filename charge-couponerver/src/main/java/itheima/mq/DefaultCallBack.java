package itheima.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
@Slf4j
public class DefaultCallBack implements SendCallback {
    private String tag;

    public DefaultCallBack(String tag) {
        this.tag = tag;
    }

    @Override
    public void onSuccess(SendResult sendResult) {
        log.info("{}发送成功！{}",tag,sendResult.getMsgId());

    }

    @Override
    public void onException(Throwable throwable) {
        log.error("{}发送失败！{}",tag,throwable.getMessage());
    }
}
