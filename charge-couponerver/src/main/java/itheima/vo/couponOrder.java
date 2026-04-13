package itheima.vo;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.sql.Date;
@Data
public class couponOrder {


    private String orderNo;          // 订单号（唯一索引，防重复消费）

    private Long userId;
    private Long couponId;
    private Integer orderStatus;     // 0-待支付 1-已完成 2-已取消
    private String traceId;          // 链路追踪ID
    private Date createTime;
    private Date updateTime;

    // 数据库唯一索引
    @TableField(exist = false)
    public static final String UNI_IDX_ORDER_NO = "uni_idx_order_no";
}
