package itheima.common;

import com.alibaba.fastjson.JSON;
import itheima.vo.RedisCouponWithExpireTime;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.convert.RedisData;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.function.Function;

import static itheima.common.consrants.RedisConstant.LOCK_COUPON_KEY;
import static itheima.common.consrants.RedisConstant.LOCK_TTL;

/**
 * 查询商品（逻辑过期）缓存
 */
@Component
@Slf4j
public class CacheClient {
    private final StringRedisTemplate stringRedisTemplate;


    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    /**
     * 线程池异步缓存重建(io密集型，cpu核数+1,CPU核数*2)
     */
    public static final ExecutorService CACHE_REBUILD_EXECUTOR = new ThreadPoolExecutor(
            5,
            10,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread();
                    t.setName("cache-rebuild-thread");
                    t.setDaemon(true);//守护线程
                    // 设置全局未捕获异常处理器
                    t.setUncaughtExceptionHandler((thread, throwable) -> {
                        log.error("线程{}发生未捕获异常", thread.getName(), throwable);
                    });
                    return t;
                }
            },
            (r, executor) -> {
                System.out.println("缓存重建任务被拒绝，线程池已满，下次自动重试");
            }
    );

    /**
     * 查询带逻辑过期的缓存，，并且带上实时热点追踪。
     *
     * // TODO redis压力有点爆缸啊(爆不爆缸啊不太清楚)，需要优化
     *
     * @param key        key前缀
     * @param id         业务id   (categoryId)
     * @param type       返回值类型
     * @param dbFallback 查询DB的函数
     * @param expireTime 逻辑过期时间
     * @param unit       时间单位
     * @param <R>        返回类型泛型
     * @param <ID>       id类型泛型
     * @return 查询结果
     */
    public <R, ID> R queryWithLogicalExpire(
            String key,
            ID id,
            Class<R> type,
            Function<ID, R> dbFallback,
            Long expireTime,
            TimeUnit unit) {
        // 1. 从redis中查询缓存
        String jsonObject = stringRedisTemplate.opsForValue().get(key);
        // 2. 判断缓存是否命中
        if (jsonObject == null) {
            log.info("【缓存未命中,请检查是否预热了缓存：{}】", key);
            return null;
        }
        RedisCouponWithExpireTime redisCouponWithExpireTime = JSON.parseObject(jsonObject, RedisCouponWithExpireTime.class);
        //拿到真实（优惠券）实体类
        R data = JSON.parseObject(JSON.toJSONString(redisCouponWithExpireTime.getData()), type);
        // 3.判断逻辑是否过期
        LocalDateTime expireTime1 = redisCouponWithExpireTime.getExpireTime();
        if (expireTime1.isAfter(LocalDateTime.now())) {
            // 3.1. 缓存未过期，直接返回
            return data;
        }
        // 3.2. 缓存过期，获取分布式锁，异步重建缓存
        log.debug("缓存逻辑过期，尝试重建，key: {}", key);
        String lockKey = LOCK_COUPON_KEY + id;
        boolean lockSuccess = tryLock(lockKey);
        if (lockSuccess){
            //再查一遍防止缓存重建
            String jsonCheck = stringRedisTemplate.opsForValue().get(key);
            RedisCouponWithExpireTime redisCouponWithExpireTime1 = JSON.parseObject(jsonCheck, RedisCouponWithExpireTime.class);
            if (redisCouponWithExpireTime1.getExpireTime().isAfter(LocalDateTime.now())){
                return JSON.parseObject(JSON.toJSONString(redisCouponWithExpireTime1.getData()), type);
            }
            //缓存没有重建--异步重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    log.info("【开始重建缓存】：{}", key);
                    R newData = dbFallback.apply(id);
                    setWithLogicalExpire(key, newData, expireTime, unit);
                    log.info("【缓存重建完成】：{}", key);
                } catch (Exception e) {
                    log.error("【缓存重建失败】：{}", key, e);
                } finally {
                    unLock(lockKey);
                }
            });
        }
        // 4.无论是否获取锁，最终都要返回旧数据
        log.info("【缓存重建中，返回旧数据】：{}", key);
        return data;
    }
    //解锁
    private void unLock(String lockKey) {
        stringRedisTemplate.delete(lockKey);
    }

    // 写入缓存
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        //封装逻辑过期的对象
        RedisCouponWithExpireTime redisCoupon = new RedisCouponWithExpireTime();
        redisCoupon.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        redisCoupon.setData(value);
        //写入redis
        stringRedisTemplate.opsForValue().set(key, JSON.toJSONString(redisCoupon));
        log.info("写入缓存（带过期时间）成功：{}，逻辑过期时间：{}", key, redisCoupon.getExpireTime());
    }
    /**
     * 尝试获取互斥锁（基于Redis SETNX）
     */
    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", LOCK_TTL, TimeUnit.SECONDS);
        // 注意：不要直接返回flag，避免自动拆箱NPE
        return BooleanUtils.isTrue(flag);
    }




}
