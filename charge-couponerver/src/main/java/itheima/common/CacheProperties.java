package itheima.common;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
//@ConfigurationProperties(prefix = "cache")
public class CacheProperties {
    /** PV阈值：分片热点缓存 */
    private Long uvThreshold2 = 1000L;
    /** 热点缓存 */
    private Long uvThreshold = 500L;
    /** 普通商品缓存TTL（分钟） */
    private Long normalTtl = 30L;

    /** 热点商品逻辑过期时间（分钟） */
    private Long hotExpireTime = 60L;

    /** 缓存分片数量 */
    private Integer shardCount = 3;

    /** 统计窗口（分钟），滑动窗口时间范围 */
    private Long statWindowMinutes = 10L;
}
