package itheima.common.consrants;

public class RedisConstant {
    //优惠券缓存key前缀
    public static final String CACHE_COUPON_KEY = "cache:coupon";
    //优惠券互斥锁key前缀
    public static final String LOCK_COUPON_KEY = "lock:coupon";
    //热点优惠券的逻辑过期时间
    public static final Long CACHE_COUPON_TTL = 30L;
    //锁的过期时间（防止死锁）
    public static final Long LOCK_TTL = 10L;


    /** 普通商品缓存 */
    public static final String CACHE_NORMAL_PRODUCT_KEY = "cache:normal:product:";

    /** 热点商品缓存（逻辑过期） */
    public static final String CACHE_HOT_PRODUCT_KEY = "cache:hot:product:";

    /** 商品分片缓存 */
    public static final String CACHE_SHARD_PRODUCT_KEY = "cache:shard:product:";

    /** 商品PV统计 */
    public static final String STAT_PRODUCT_PV_KEY = "stat:pv:product:";

    /** 商品UV统计（HyperLogLog） */
    public static final String STAT_PRODUCT_UV_KEY = "stat:uv:product:";

    /** 热点商品标记 */
    public static final String HOT_PRODUCT_FLAG_KEY = "hot:product:flag:";

    /** 商品互斥锁 */
    public static final String LOCK_PRODUCT_KEY = "lock:product:";

    /** 升级锁（防止重复升级） */
    public static final String LOCK_UPGRADE_KEY = "lock:upgrade:product:";
}
