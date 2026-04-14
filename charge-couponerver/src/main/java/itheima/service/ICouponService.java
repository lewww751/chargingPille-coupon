package itheima.service;

import itheima.vo.dto.CouponDetailDTO;
import itheima.vo.dto.CouponListDTO;

import java.util.List;


public interface ICouponService {

    List<CouponListDTO> getCouponList();

    CouponDetailDTO getCouponDetail(Integer id);

    Long buyCoupon(int category);
    Long buyCouponV2(int category);
}
