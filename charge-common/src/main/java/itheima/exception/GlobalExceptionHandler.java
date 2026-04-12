package itheima.exception;


import itheima.result.Result;
import itheima.result.ResutCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.BindException;
//import org.springframework.validation.BindException;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMsg());
        return Result.fail(e.getMsg());
    }

    /** 参数校验异常 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public Result<?> handleValidation(Exception e) {
        String msg = e instanceof MethodArgumentNotValidException ex
                ? ex.getBindingResult().getFieldErrors().get(0).getDefaultMessage()
                : e.getMessage();
        return Result.fail(ResutCode.PARAM_ERROR.getMsg() + ": " + msg);
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统异常: ", e);
        return Result.fail(ResutCode.SERVER_ERROR);
    }
}
