package itheima.detect;

import com.alibaba.fastjson.JSON;
import itheima.vo.HotDataEvent;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * 检测热点数据
 */
@Slf4j
@Service
public class hotDataDetector {
    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final int HOT_THRESHOLD = 100; // 1分钟访问≥100次=热点
    private static final int WINDOW_SIZE = 60;    // 时间窗口：60秒

    private static final String ACCESS_COUNTER_PREFIX = "access:counter:";
    private static final String ACCESS_UV_PREFIX = "access:uv:";
    private static final String HOT_DATA_PREFIX = "hot:data:";
    private static final String HOT_RANKING_KEY = "hot:data:ranking";
    private static final String HOT_EVENT_CHANNEL = "hot:data:channel";


    /**
     * Lua脚本：原子自增 + 设置过期时间（解决竞态条件）
     */
    private static final DefaultRedisScript<Long> INCR_EXPIRE_SCRIPT;

    static {
        INCR_EXPIRE_SCRIPT = new DefaultRedisScript<>();
        INCR_EXPIRE_SCRIPT.setScriptText(
                "local count = redis.call('incr', KEYS[1]) " +
                        "if tonumber(count) == 1 then " +
                        "    redis.call('expire', KEYS[1], ARGV[1]) " +
                        "end " +
                        "return count"
        );
        INCR_EXPIRE_SCRIPT.setResultType(Long.class);
    }
    /**
     * 访问并判断是否成为热点数据
     * @param key
     */
    public boolean recordAccessAndCheckHot(String key, String userId){
//        String Key = "access:count:" + key;
        countUv(key, userId);
        // 2. 滑动窗口访问计数（原子操作，无并发问题）
        Long count = incrementWithExpire(key);
        if (count != null && count >= HOT_THRESHOLD && !isHot(key)){
            markAsHotData(key,count);
            log.info("【热点数据检测】{} 成为热点数据，热度值：{}", key, count);
            return true;
        }
        return false;
    }

    private boolean isHot(String key) {
        String hotKey = HOT_DATA_PREFIX + key;
        return redisTemplate.hasKey(hotKey);
    }

    private void markAsHotData(String key, Long count) {
        String hotKey = HOT_DATA_PREFIX + key;
        redisTemplate.opsForZSet().add(HOT_RANKING_KEY, key, count);
        // 2. 设置热点标记（过期时间）
        redisTemplate.opsForValue().set(hotKey, "1", 10, TimeUnit.MINUTES);
        // 3. 发布热点事件（解耦业务：缓存预热、限流、降级）
        publishHotDataEvent(key, count);
    }
    /**
     * 发布热点事件
     */
    private void publishHotDataEvent(String key, long count) {
        HotDataEvent event = new HotDataEvent(key, count, System.currentTimeMillis());
        redisTemplate.convertAndSend(HOT_EVENT_CHANNEL, JSON.toJSONString(event));
    }

    /**
     * 原子自增 + 过期时间（Lua）
     */
    private Long incrementWithExpire(String key) {
        String counterKey = ACCESS_COUNTER_PREFIX + key;
        return redisTemplate.execute(
                INCR_EXPIRE_SCRIPT,
                Collections.singletonList(counterKey),
                String.valueOf(WINDOW_SIZE)
        );
    }

    /**
     * 记录UV，单个用户点击多次只算 一次
     */
    private void countUv(String key, String userId) {
        String uvKey = ACCESS_UV_PREFIX + key;
        redisTemplate.opsForHyperLogLog().add(uvKey, userId);
        // 设置过期时间，自动清理
        redisTemplate.expire(uvKey, WINDOW_SIZE, TimeUnit.SECONDS);
    }
}
