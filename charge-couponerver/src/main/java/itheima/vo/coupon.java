package itheima.vo;

import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.sql.Date;

@Data
public class coupon {
    private Integer categoryId;
    private String name;
    private Integer totalCount;      // 总库存
    private Integer remainCount;     // 剩余库存
    private Integer scope;
    private Date expireDate;
    @Version
    private Integer version;         // 乐观锁版本号
    private Date createTime;
    private Date updateTime;
}
