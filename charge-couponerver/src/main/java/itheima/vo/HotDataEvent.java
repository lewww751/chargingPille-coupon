package itheima.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HotDataEvent {
    /**
     * 热点数据key
     */
    private String key;
    /**
     * 访问次数
     */
    private long count;
    /**
     * 触发时间
     */
    private long timestamp;
}
