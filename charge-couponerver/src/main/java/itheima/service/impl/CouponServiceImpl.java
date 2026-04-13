package itheima.service.impl;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import itheima.mapper.CouponMapper;
import itheima.service.ICouponService;
import itheima.vo.*;
import itheima.vo.dto.CouponDetailDTO;
import itheima.vo.dto.CouponListDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.IdGenerator;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import com.github.benmanes.caffeine.cache.Cache;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CouponServiceImpl extends ServiceImpl<CouponMapper, CouponW> implements ICouponService{
    @Resource
    private RedisScript<Long> redisScript;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private Cache<String, Object> caffeineCache;
    private final CouponMapper couponMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public CouponServiceImpl(CouponMapper couponMapper, RedisTemplate<String, Object> redisTemplate) {
        this.couponMapper = couponMapper;
        this.redisTemplate = redisTemplate;
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
    @Override
    public Long buyCoupon(int categoryId) {
        UserInfo userInfo = getUserInfo();
        log.info("【购买优惠券】用户信息：{}", userInfo);
        //1.判断用户是否已经下过订单
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
     * 扣减库存
     */
    @CacheEvict(key = "'coupons:detail:' + #categoryId" )
    public void decrStockCount(int categoryId) {
        // 获取分布式锁
        String key = "seckill:coupon:stockdecr" + categoryId;
        try {
            int count = 0;
            long ret = 0;
            do{
                 ret = redisTemplate.execute(redisScript, Collections.singletonList(key), 1, 10);
                 if (ret > 1){
                     break;
                 }
                 count++;
                 if (count >= 3){
                     log.error("【购买优惠券获取分布式锁失败，达到最大重试次数】");
                     return;
                 }
                 Thread.sleep(20);
             }while (true);

            couponMapper.decrStockCount(categoryId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            redisTemplate.delete(key);
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
}
