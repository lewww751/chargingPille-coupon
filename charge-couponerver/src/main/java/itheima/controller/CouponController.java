package itheima.controller;


import itheima.result.Result;
import itheima.service.ICouponService;
import itheima.vo.CouponVo;
import itheima.vo.dto.CouponDetailDTO;
import itheima.vo.dto.CouponListDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/home")
public class CouponController {

    @Autowired
    private ICouponService ICouponService;

    /**
     * 查询优惠券列表
     * @return
     */
    @GetMapping("/data")
    public Result<List<CouponListDTO>> getCoupon() {
        List<CouponListDTO> couponList = ICouponService.getCouponList();
        log.info("【/home/data运行完毕】couponList:{}", couponList);
        return Result.ok(couponList);
    }
    /**
     * 查询优惠券详细信息
     */
    @GetMapping("/detail/{id}")
    public Result<CouponDetailDTO> getCouponDetail(@PathVariable Integer id) {
        CouponDetailDTO couponDetail = ICouponService.getCouponDetail(id);
        log.info("【/home/detail运行完毕】couponDetail:{}", couponDetail);
        return Result.ok(couponDetail);
    }
}
