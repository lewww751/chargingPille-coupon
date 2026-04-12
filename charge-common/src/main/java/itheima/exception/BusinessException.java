package itheima.exception;

import itheima.result.ResutCode;
import lombok.Getter;

/**
 * 自定义业务异常类
 * 用于抛出业务逻辑中的异常，携带状态码和消息
 */
@Getter // 自动生成getMsg()和getCode()方法
public class BusinessException extends RuntimeException {

    private final Integer code;
    private final String msg;

    /**
     * 构造方法1：基于状态码枚举
     */
    public BusinessException(ResutCode resultCode) {
        super(resultCode.getMsg()); // 父类存消息
        this.code = resultCode.getCode();
        this.msg = resultCode.getMsg();
    }

    /**
     * 构造方法2：自定义消息（默认失败码）
     */
    public BusinessException(String msg) {
        super(msg);
        this.code = ResutCode.FAIL.getCode();
        this.msg = msg;
    }

    /**
     * 构造方法3：自定义状态码+消息
     */
    public BusinessException(Integer code, String msg) {
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
