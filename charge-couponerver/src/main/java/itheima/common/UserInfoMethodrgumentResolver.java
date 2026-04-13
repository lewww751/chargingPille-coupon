package itheima.common;

import itheima.common.Aop.RequestUser;
//import org.jboss.marshalling.TraceInformation;
import itheima.vo.UserInfo;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.HandlerMethodArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

public  class UserInfoMethodrgumentResolver implements HandlerMethodArgumentResolver {
    /**
     * 作用范围
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(UserInfo.class) &&
                parameter.hasParameterAnnotation(RequestUser.class);
    }

    // TODO 待完善获取用户信息
    @Override
    public Object resolveArgument(MethodParameter parameter, Message<?> message) throws Exception {
        String token = message.getHeaders().get("header").toString();
        return null;
    }


}
