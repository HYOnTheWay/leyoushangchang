package com.leyou.cart.interceptor;


import com.leyou.cart.config.JwtProperties;
import com.leyou.common.utils.CookieUtils;
import com.leyou.pojo.UserInfo;
import com.leyou.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j

public class UserInterceptor implements HandlerInterceptor {

    private JwtProperties prop;

    private static final ThreadLocal<UserInfo> thread = new ThreadLocal<>();

    public UserInterceptor(JwtProperties prop) {
        this.prop = prop;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        //获取cookie
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        try {
            //解析token
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            //传递user
            thread.set(user);  //线程传递数据到controller中
            //放行
            return true;
        }catch (Exception e){
            log.error("[购物车服务] 解析用户身份失败",e);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        //最后用完数据一定要清空
        thread.remove();
    }

    public static UserInfo getUser(){
        return thread.get();
    }
}
