package itheima.service.impl;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import itheima.common.CacheClient;
import itheima.common.CacheProperties;
import itheima.mapper.CouponMapper;
import itheima.mq.DefaultCallBack;
import itheima.mq.MQConstant;
import itheima.service.ICouponService;
import itheima.vo.*;
import itheima.vo.dto.CouponDetailDTO;
import itheima.vo.dto.CouponListDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.SymbolicLinkFileFilter;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.redisson.api.IdGenerator;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static itheima.common.consrants.RedisConstant.*;

@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, CouponW> implements ICouponService{
    @Resource
    private RocketMQTemplate rocketMQTemplate;
    private final CacheProperties cacheProperties;
    private final StringRedisTemplate RedisTemplate;
    private final CacheClient cacheClient;
    /**
     * 买完的标识
     */
    private static final Map<Long,Boolean> STOCK_OVER_FLOW_MAP = new ConcurrentHashMap<>();



    private final ScheduledExecutorService scheduledExecutorService;

    @Resource
    private RedisScript<Boolean> redisScript;
    private final RedissonClient redissonClient;
    private final Cache<String, Object> caffeineCache;
    private final CouponMapper couponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public CouponServiceImpl(CouponMapper couponMapper, RedisTemplate<String, Object> redisTemplate, RedissonClient redissonClient, Cache<String, Object> caffeineCache, ScheduledExecutorService scheduledExecutorService, StringRedisTemplate RedisTemplate, CacheClient cacheClient, CacheProperties cacheProperties) {
        this.couponMapper = couponMapper;
        this.redisTemplate = redisTemplate;
        this.redissonClient = redissonClient;
        this.caffeineCache = caffeineCache;
        this.scheduledExecutorService = scheduledExecutorService;
        this.RedisTemplate = RedisTemplate;
        this.cacheClient = cacheClient;
        this.cacheProperties = cacheProperties;
    }
    public static void deleteKey(String key){
        STOCK_OVER_FLOW_MAP.remove(Long.valueOf(key));
    }

    /**
     * 应用启动时预热热点数据
     */
    // TODO 预热热点数据，后期用spring框架来完成----->自动根据前一天的数据进行预热，采用定时任务

    @PostConstruct
    public void warmUpHotData() {
        log.info("【postConstruct】开始预热热点优惠券数据...】");
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            // 查询热点优惠券
            List<CouponVo> hotCoupons = couponMapper.selectHotCoupons();
            log.info("【热点数据预热】，共{}条", hotCoupons.size());
            if (hotCoupons != null && !hotCoupons.isEmpty()){
                //转化成DTO
                List<CouponListDTO> couponListDTOS = convertToDTO(hotCoupons);
                String jsonStr = objectMapper.writeValueAsString(couponListDTOS);
                //缓存布隆过滤器
                List<Long> categoryIDs = couponListDTOS.stream().map(s -> s.getCategoryId()).collect(Collectors.toList());
                RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("bloom:coupon:category:ids");
                bloomFilter.tryInit(1000000, 0.01);
                categoryIDs.forEach(bloomFilter::add);
                log.info("【缓存布隆过滤器】，共{}条", categoryIDs.size());
                //  缓存本地数据
                    String HOT_COUPONS_KEY = "coupons:hot:list";//缓存热点数据
                    caffeineCache.put(HOT_COUPONS_KEY, couponListDTOS);
                    //缓存Redis数据
                    redisTemplate.opsForValue().set(HOT_COUPONS_KEY, jsonStr, 20, TimeUnit.DAYS);
                log.info("【热点数据预热完成】，共{}条", hotCoupons.size());
            }
        } catch (Exception e) {
            log.error("热点数据预热失败", e);
        }
    }


    /**
     * 获取优惠券列表
     * @return
     */
    @SuppressWarnings("unchecked")// 忽略类型转换警告
    public List<CouponListDTO> getCouponList() {
        int maxRetries = 3;
        int retryCount = 0;
        String HOT_COUPONS_KEY = "coupons:hot:list";
        String CACHE_UPDATE_LOCK = "coupons:cache:update:lock";
        String cacheKey = "coupon:list";//缓存所有的key
        log.info("【获取优惠券列表】开始");
        //1.先从本地缓存获取
        Object hotCouponsFromCaffine = caffeineCache.getIfPresent(cacheKey);
        if (hotCouponsFromCaffine != null ) {
            log.info("【获取优惠券列表】从Caffeine缓存获取数据成功");
            return (List<CouponListDTO>) hotCouponsFromCaffine;
        }
        //2.再从Redis缓存获取
        Object hotCouponsFromRedis = redisTemplate.opsForValue().get(cacheKey);
        if (hotCouponsFromRedis != null) {
            log.info("【获取优惠券列表】从Redis缓存获取数据成功");
            //回写caffeine缓存
            List<CouponListDTO> hotCouponsListDTOFromRedis = (List<CouponListDTO>) hotCouponsFromRedis;
            caffeineCache.put(cacheKey, hotCouponsListDTOFromRedis);
            return hotCouponsListDTOFromRedis;
        }
        //3.缓存中没有再从数据库获取
        //3.1先获取分布式锁，防止缓存击穿
        RLock lock = redissonClient.getLock(CACHE_UPDATE_LOCK + cacheKey);
        try {
            //获得锁，最多等待5秒，锁上10秒后自动释放
            if (lock.tryLock(5, TimeUnit.SECONDS)){
                try {
                    log.info("【获取优惠券列表】获取分布式锁成功");
                    //双重检测，防止重复查询
                    hotCouponsFromRedis= redisTemplate.opsForValue().get(cacheKey);
                    if (hotCouponsFromRedis != null){
                        return (List<CouponListDTO>) hotCouponsFromRedis;
                    }
                    //3.2从数据库获取数据
                    List<CouponVo> couponVoList = couponMapper.selectAllEnabled();
                    log.info("【获取优惠券列表】从数据库获取数据成功，数据是：{}", couponVoList);
                    //3.3防止缓存穿透，缓存空数据
                    if (couponVoList == null || couponVoList.isEmpty()){
                        redisTemplate.opsForValue().set(cacheKey,List.of(),10, TimeUnit.MINUTES);
                        return List.of();
                    }
                    //布隆过滤器过滤
                    List<Long> categoryIDs = couponVoList.stream().map(s -> s.getCategoryId()).collect(Collectors.toList());
                    RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("bloom:coupon:category:ids");
                    bloomFilter.tryInit(1000000, 0.01);
                    categoryIDs.forEach(bloomFilter::add);
                    log.info("【缓存布隆过滤器】，共{}条", categoryIDs.size());
                    List<CouponListDTO> couponListDTOS = convertToDTO(couponVoList);
                    redisTemplate.opsForValue().set(cacheKey, couponListDTOS, 10, TimeUnit.MINUTES);
                    caffeineCache.put(cacheKey, couponListDTOS);
                    return couponListDTOS;
                } finally {
                    lock.unlock();
                }
            }else {
                log.info("【获取优惠券列表】获取分布式锁失败");
                // 等待100ms后重试
                Thread.sleep(100);
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("【获取优惠券列表】获取分布式锁失败，达到最大重试次数");
                    //降级查询数据库
                    return convertToDTO(couponMapper.selectAllEnabled());
                }
                // 递归调用，等待锁
                return getCouponList();
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁异常", e);
            Thread.currentThread().interrupt();
            // 降级处理：直接查询数据库
            return convertToDTO(couponMapper.selectAllEnabled());
        }
    }

    /**
     * 查询指定优惠券详情
     cagegoryId
     */
    @Override
    public CouponDetailDTO getCouponDetail(Integer id) {
        String COUPON_CACHE_PREFIX = "coupons:detail:";
        String CACHE_UPDATE_LOCK = "coupons:update:lock";
        log.info("【获取优惠券详情】开始");
        String cacheKey = COUPON_CACHE_PREFIX + id;
        //先在布隆过滤器判断
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("bloom:coupon:category:ids");
        if (!bloomFilter.contains(id)){
            log.info("【获取优惠券详情】布隆过滤器判断优惠券不存在");
            return null;
        }
        //1.先从caffine缓存中获取
        Object couponDetail = caffeineCache.getIfPresent(cacheKey);
        if (couponDetail != null){
            log.info("【获取优惠券详情从Caffeine缓存获取数据成功】");
            return (CouponDetailDTO) couponDetail;
        }
        //2.再从Redis缓存中获取
        Object couponDetailFromRedis = redisTemplate.opsForValue().get(cacheKey);
        if (couponDetailFromRedis != null && couponDetailFromRedis != "NULL"){
            log.info("【获取优惠券详情从Redis缓存获取数据成功】");
            //回写caffine缓存
            CouponDetailDTO dto = (CouponDetailDTO) couponDetailFromRedis;
            caffeineCache.put(cacheKey, dto);
            return dto;
        }
        //3.缓存中没有再从数据库获取
        //3.1先获取分布式锁，防止缓存击穿
        RLock lock = redissonClient.getLock(CACHE_UPDATE_LOCK + cacheKey);
        int maxRetries = 3;
        int retryCount = 0;
        try {
            //获得锁，最多等待5秒，锁上10秒后自动释放
            if (lock.tryLock(5, TimeUnit.SECONDS)){
                try {
                    log.info("【获取优惠券列表】获取分布式锁成功");
                    //双重检测，防止重复查询
                    couponDetailFromRedis= redisTemplate.opsForValue().get(cacheKey);
                    if (couponDetailFromRedis != null){
                        return (CouponDetailDTO) couponDetailFromRedis;
                    }
                    //3.2从数据库获取数据
                    CouponDetailDTO couponDetailDTO = couponMapper.selectByCategoryId(id);
                    log.info("【获取优惠券列表】从数据库获取数据成功，数据是：{}", couponDetailDTO);
                    //3.3防止缓存穿透，缓存空数据
                    if (couponDetailDTO == null){
                        redisTemplate.opsForValue().set(cacheKey,"NULL",10, TimeUnit.MINUTES);
                        return null;
                    }
                    redisTemplate.opsForValue().set(cacheKey, couponDetailDTO, 10, TimeUnit.MINUTES);
                    caffeineCache.put(cacheKey, couponDetailDTO);
                    return couponDetailDTO;
                } finally {
                    lock.unlock();
                }
            }else {
                log.info("【获取优惠券列表】获取分布式锁失败");
                // 等待100ms后重试
                Thread.sleep(100);
                retryCount++;
                if (retryCount >= maxRetries) {
                    log.error("【获取优惠券列表】获取分布式锁失败，达到最大重试次数");
                    //降级查询数据库
                    return couponMapper.selectByCategoryId(id);
                }
                // 递归调用，等待锁
                return getCouponDetail(id);
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁异常", e);
            Thread.currentThread().interrupt();
            // 降级处理：直接查询数据库
            return couponMapper.selectByCategoryId(id);
        }
    }

    /**
     * 购买接口
     * @param categoryId
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long buyCoupon(int categoryId) {
        UserInfo userInfo = getUserInfo();
        log.info("【购买优惠券】用户信息：{}", userInfo);
        //1.判断用户是否已经下过订单
        if (couponMapper.existsOrderByUserId(userInfo.getPhone())) {
            log.error("【购买优惠券】【用户已经下过订单，不能重复购买】");
            return null;
        }
        //2.查询优惠券信息--库存是否充足
          //2.1查询优惠券信息
        CouponW couponW = couponMapper.selectById(categoryId);
          //2.2判断库存是否充足
        if (couponW.getRemainCount() <= 0) {
            log.error("【购买优惠券】库存不足， categoryId: {}", categoryId);
            return null;
        }
        //3.创建订单，扣除库存，返回订单id
        decrStockCount(categoryId);
        OrderInfo orderInfo = buildOrderInfo(userInfo, couponW);
        return orderInfo.getOrderId();
    }

    /**
     * 购买接口V2--------乐观锁
     * @param category
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
//    @CacheEvict(key = "'coupons:detail:' + #category" )
    @Override
    public String buyCouponV2(int category) {
        Boolean isDone = STOCK_OVER_FLOW_MAP.get((long) category);
        if (isDone != null && isDone) {
            log.error("【购买优惠券】库存不足， categoryId: {}", category);
            return null;
        }
        CouponW couponW = couponMapper.selectById(category);
        UserInfo userInfo = getUserInfo();
        log.info("【购买优惠券】用户信息：{}", userInfo);
        //1.判断用户是否已经下过订单
        String userOrderFlag = "seckill:user:order:" + couponW.getCategoryId();
        Long isOrdered = redisTemplate.opsForHash().increment(userOrderFlag, userInfo.getPhone() + "", 1);
        if (isOrdered <= 1){
            log.error("【购买优惠券】【用户已经下过订单，不能重复购买】");
            return null;
        }
//        if (couponMapper.existsOrderByUserId(userInfo.getPhone())) {
//            log.error("【购买优惠券】【用户已经下过订单，不能重复购买】");
//            return null;
//        }
        try {
            //判断库存是否充足
            String hashKey = "seckill:coupons:stock:" + category;
            Long remain = redisTemplate.opsForHash().increment(hashKey, category + "", -1);
            if (remain < 0){
                log.error("【购买优惠券】库存不足， categoryId: {}", category);
                return null;
            }

//        //判断库存是否充足
//        if (couponW.getRemainCount() <= 0) {
//            log.error("【购买优惠券】库存不足， categoryId: {}", category);
//            return null;
//        }
            // 扣减库存,发送MQ
//            decrStockCountv2(category);
            rocketMQTemplate.asyncSend(MQConstant.ORDER_PENDING_TOPIC,
                    new OrderMessage((long) category,userInfo.getPhone(),null, null)
                    ,new DefaultCallBack("发送订单MQ"));


        } catch (Exception e) {
            //买完的本地标识
            STOCK_OVER_FLOW_MAP.put((long) category, true);
            //删除用户重复下单表示
            redisTemplate.opsForHash().delete(userOrderFlag, userInfo.getPhone() + "");
            throw new RuntimeException(e);
        }
        //OrderInfo orderInfo = buildOrderInfo(userInfo, couponW);
        return "正在抢票中";
    }




    /**
     * 查询指定优惠券详情--V2（逻辑过期）（实时热点发现）
     */
    // 异步处理线程池（统计和升级）
    private static final ExecutorService ASYNC_EXECUTOR =
            new ThreadPoolExecutor(
                    3, 8, 60, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(200),
                    r -> {
                        Thread t = new Thread(r, "product-stat-thread");
                        t.setDaemon(true);
                        return t;
                    }
            );
    @Override
    public CouponDetailDTO getCouponDetailNew(Integer categoryId) {
        UserInfo userInfo = getUserInfo();
        //布隆过滤器
        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter("bloom:coupon:category:ids");

        if (!bloomFilter.contains(categoryId)){
            log.info("【获取优惠券详情】布隆过滤器判断优惠券不存在");
            return null;
        }

        //查询
//        CouponDetailDTO couponDetailDTO = cacheClient.queryWithLogicalExpire(
//                CACHE_COUPON_KEY,   // key前缀
//                (long) categoryId,                  // 商品id
//                CouponDetailDTO.class,       // 返回类型
//                this::getFromDB,     // DB查询函数
//                CACHE_COUPON_TTL,   // 逻辑过期时间30分钟
//                TimeUnit.MINUTES
//        );
        ASYNC_EXECUTOR.submit(() -> {
            try {
                recordAccess((long) categoryId, (long) userInfo.getPhone());
                checkAndUpgradeCache((long) categoryId);
            } catch (Exception e) {
                log.error("【商品访问活跃度统计】商品访问活跃度统计异常", e);
            }
        });
        // 2. 判断是否热点商品，路由到不同缓存策略
        if (isHot2Product(categoryId)) {
            log.debug("分片热点商品，走分片逻辑过期缓存，productId: {}", categoryId);
            return getFromShardCache(categoryId);
        } else if (isHot1Product(categoryId)){
            log.debug("普通热点商品，走普通逻辑过期缓存，productId: {}", categoryId);
            return getFromNormalCache(categoryId);
        }else {
            log.debug("普通商品，走普通缓存，productId: {}", categoryId);
            return getFromNormal(categoryId);
        }
//        if (couponDetailDTO == null){
//            log.info("【获取优惠券详情】商品未预热，key={}",categoryId);
//        }

    }

    /**
     * 从普通缓存中拿到数据
     * @param categoryId
     * @return
     */
    private CouponDetailDTO getFromNormal(Integer categoryId) {
        return null;
    }

    /**
     * 从普通热点缓存中拿到数据
     * @param categoryId
     * @return
     */
    private CouponDetailDTO getFromNormalCache(Integer categoryId) {
        return null;
    }

    /**
     * 是否是普通热点商品
     * @param categoryId
     * @return
     */
    private boolean isHot1Product(Integer categoryId) {
            return false;
    }

    /**
     * 从分片商品中拿到数据
     * @param categoryId
     * @return
     */
    private CouponDetailDTO getFromShardCache(Integer categoryId) {
        return null;
    }

    /**
     * 是否是分片热点商品
     * @param categoryId
     * @return
     */
    private boolean isHot2Product(Integer categoryId) {
        return false;
    }

    private CouponDetailDTO getFromDB(Long integer) {
        CouponW couponW = couponMapper.selectById(integer);
        CouponDetailDTO couponDetailDTO = new CouponDetailDTO();
        BeanUtils.copyProperties(couponW, couponDetailDTO);
        return couponDetailDTO;
    }
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "'coupons:detail:' + #category" )
    public Long decrStockCountv2(int category) {
        int row = couponMapper.decrStockCountv2(category);
        if (row == 0) {
            log.error("【购买优惠券】库存不足， categoryId: {}", category);
            return null;
        }
        CouponW couponW = couponMapper.selectById(category);
        UserInfo userInfo = getUserInfo();
        OrderInfo orderInfo = buildOrderInfo(userInfo, couponW);
        return orderInfo.getOrderId();
    }

    /**
     * 生成订单
     * @return
     */
    private OrderInfo buildOrderInfo(UserInfo userInfo, CouponW couponW) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setUserPhone(userInfo.getPhone());
        orderInfo.setOrderPrice(couponW.getPrice());
        orderInfo.setCategoryId(couponW.getCategoryId());
        orderInfo.setOrderId(IdWorker.getId());
        orderInfo.setOrderTime(new Date());
        orderInfo.setUserName(userInfo.getUsername());
        return orderInfo;
    }

    // TODO 获取用户信息
    /**
     * 错误的---应该由token获取
     */
    private static UserInfo getUserInfo() {
        UserInfo userInfo = new UserInfo();
        userInfo.setPhone(18265958585L);
        userInfo.setUsername("user");
        userInfo.setPassword("password");
        return userInfo;
    }

    /**
     * 扣减库存-------悲观锁
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(key = "'coupons:detail:' + #categoryId" )
    public void decrStockCount(int categoryId) {
        // 拿到分布式唯一线程id
        String threadId = IdWorker.getId() + "";
        int timeout = 10;
        ScheduledFuture<?> future = null;
        // 获取分布式锁
        String key = "seckill:coupon:stockdecr" + categoryId;
        try {
            int count = 0;
            Boolean ret ;
            do{
                 ret = redisTemplate.execute(redisScript, Collections.singletonList(key), threadId, "10");
                 if (ret != null && ret){
                     break;
                 }
                 count++;
                 if (count >= 3){
                     log.error("【购买优惠券获取分布式锁失败，达到最大重试次数】");
                     return;
                 }
                 Thread.sleep(20);
             }while (true);
            // watchDog 自定续期
            long delayTime = (long) (timeout * 0.8);
            future = scheduledExecutorService.scheduleAtFixedRate(() -> {
                String value = (String) redisTemplate.opsForValue().get(key);
                if (threadId.equals(value)) {
                    //续期
                    redisTemplate.expire(key, delayTime + 2, TimeUnit.SECONDS);
                    return;
                }
                // 如果不存在，停止线程不再执行
                Thread.currentThread().interrupt();
            }, delayTime, delayTime, TimeUnit.SECONDS);
            couponMapper.decrStockCount(categoryId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 停止线程
            if (future != null){
                future.cancel(true);
            }
            //释放 线程id 相同的锁
            String value = (String) redisTemplate.opsForValue().get(key);
            if (value != null && value.equals(threadId)){
                redisTemplate.delete(key);
            }
        }
    }


    // TODO 更新优惠券接口，如更新优惠券状态、删除优惠券等


    /**
     * 实体转化DTO
     */
    private List<CouponListDTO> convertToDTO(List<CouponVo> hotCoupons) {
        return hotCoupons.stream().map(couponVo -> {
            CouponListDTO couponListDTO = new CouponListDTO();
            BeanUtils.copyProperties(couponVo, couponListDTO);
            return couponListDTO;
        }).collect(Collectors.toList());

    }
    /**
     * 记录商品访问（PV + UV）
     */
    public void recordAccess(Long productId, Long  userId) {
        // 获取当前时间窗口（滑动窗口，精确到分钟）
        String timeWindow = getCurrentTimeWindow();

        CompletableFuture<Void> uvFuture = CompletableFuture.runAsync(
                () -> recordUV(productId, userId, timeWindow)
        );

        CompletableFuture.allOf(uvFuture).join();
    }
    /**
     * 记录UV（独立访客数）
     * 使用HyperLogLog（内存占用极小，允许少量误差）
     */
    private void recordUV(Long productId, Long userId, String timeWindow) {
        // key格式：stat:uv:product:{productId}:{timeWindow}
        String uvKey = STAT_PRODUCT_UV_KEY + productId + ":" + timeWindow;
        redisTemplate.opsForHyperLogLog().add(uvKey, userId);

        // 设置过期时间
        redisTemplate.expire(
                uvKey,
                cacheProperties.getStatWindowMinutes() + 5,
                TimeUnit.MINUTES
        );
        log.debug("UV记录，productId: {}, userId: {}", productId, userId);
    }
    /**
     * 获取商品UV（当前时间窗口）
     */
    public Long getProductUV(Long productId) {
        String timeWindow = getCurrentTimeWindow();
        String uvKey = STAT_PRODUCT_UV_KEY + productId + ":" + timeWindow;
        return redisTemplate.opsForHyperLogLog().size(uvKey);
    }
    /**
     * 获取当前时间窗口（精确到分钟）
     */
    private String getCurrentTimeWindow() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }
    /**
     * 检查并升级缓存（普通 热点 分片热点）
     */
    private void checkAndUpgradeCache(Long productId) {
        // 已经是热点，不重复升级
//        if (isHotProduct(productId)) {
//            return;
//        }
        Long uv = getProductUV(productId);

        boolean needUpgradeTo1 = uv >= cacheProperties.getUvThreshold();

        if (!needUpgradeTo1) {
            return;
        }

        // 获取升级锁，防止多线程重复升级
        String lockKey = LOCK_UPGRADE_KEY + productId;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", 30, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            return; // 其他线程正在升级
        }

        try {
            log.info("商品达到热点阈值，开始升级缓存，productId: {}, UV: {}",
                    productId, uv);
            // 查询DB获取最新数据
            CouponDetailDTO product = getFromDB(productId);
            if (product == null) {
                return;
            }
            // 写入分片逻辑过期缓存
            writeShardCache(
                    productId,
                    product,
                    cacheProperties.getHotExpireTime()
            );

            // 标记为热点商品
//            statService.markAsHotProduct(productId);

            log.info("缓存升级完成，productId: {} 已升级为热点分片缓存", productId);

        } finally {
            redisTemplate.delete(lockKey);
        }
    }
    /**
     * 写入分片缓存
     * 将同一商品数据复制到多个分片，分散查询压力
     *
     * @param productId  商品ID
     * @param product    商品数据
     * @param expireTime 逻辑过期时间
     */
    public void writeShardCache(Long productId, CouponDetailDTO product, Long expireTime) {
        int shardCount = cacheProperties.getShardCount();
        log.info("写入分片缓存，productId: {}, 分片数: {}", productId, shardCount);

        for (int i = 0; i < shardCount; i++) {
            String shardKey = buildShardKey(productId, i);
            cacheClient.setWithLogicalExpire(
                    shardKey,
                    product,
                    expireTime,
                    TimeUnit.MINUTES
            );
            log.debug("分片写入完成，key: {}", shardKey);
        }
    }
    /**
     * 构建分片Key
     * 格式：cache:shard:product:{productId}:shard{index}
     */
    public String buildShardKey(Long productId, int shardIndex) {
        return CACHE_SHARD_PRODUCT_KEY + productId + ":shard" + shardIndex;
    }
    /**
     * 删除所有分片缓存（数据更新时调用）
     */
    public void deleteShardCache(Long productId) {
        int shardCount = cacheProperties.getShardCount();
        for (int i = 0; i < shardCount; i++) {
            String shardKey = buildShardKey(productId, i);
            redisTemplate.delete(shardKey);
        }
        log.info("分片缓存已清除，productId: {}", productId);
    }

    /**
     * 下单失败得回补操作
     * @param orderMessage
     */
    public void failedRollBack(OrderMessage orderMessage) {
        //把redis里的库存数量再查一遍
        Long stockCount = couponMapper.selectStockById(orderMessage.getCouponId());
        String hashKey = "seckill:coupons:stock:" + orderMessage.getCouponId();
        redisTemplate.opsForHash().put(hashKey, orderMessage.getCouponId(), stockCount);
        //删除本地下过单的标识
        String userOrderFlag = "seckill:user:order:" + orderMessage.getCouponId();
        redisTemplate.opsForHash().delete("seckill:users:order:" + orderMessage.getUserPhone());
        //删除本地数量缓存
        deleteKey(orderMessage.getCouponId().toString());
    }
}
