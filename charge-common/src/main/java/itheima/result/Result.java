package itheima.result;

import lombok.Data;
import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private Integer code;
    private String  msg;
    private T       data;
    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = ResutCode.SUCCESS.getCode();
        r.msg  = ResutCode.SUCCESS.getMsg();
        r.data = data;
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(String msg) {
        Result<T> r = new Result<>();
        r.code = ResutCode.FAIL.getCode();
        r.msg  = msg;
        return r;
    }

    public static <T> Result<T> fail(ResutCode code) {
        Result<T> r = new Result<>();
        r.code = code.getCode();
        r.msg  = code.getMsg();
        return r;
    }
}
