package com.leyou.filters;

import com.leyou.common.utils.CookieUtils;
import com.leyou.config.FilterProperties;
import com.leyou.config.JwtProperties;
import com.leyou.pojo.UserInfo;
import com.leyou.utils.JwtUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@EnableConfigurationProperties({JwtProperties.class, FilterProperties.class})
public class AuthFilter extends ZuulFilter {

    @Autowired
    private JwtProperties prop;

    @Autowired
    private FilterProperties filterPrope;

    @Override
    public String filterType() {
        return FilterConstants.PRE_TYPE; //前置过滤
    }

    @Override
    public int filterOrder() {
        return FilterConstants.PRE_DECORATION_FILTER_ORDER - 1; //过滤器顺序
    }

    /**
     * 是否过滤
     * @return
     */
    @Override
    public boolean shouldFilter() {
        //获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = ctx.getRequest();
        //获取请求的url路径
        String path = request.getRequestURI();
        //判断是否放行
        return !isAllowPath(path);
    }

    /**
     * 判断路径方法
     * @param path
     * @return
     */
    private Boolean isAllowPath(String path) {
        for (String allowPath : filterPrope.getAllowPaths()) {
            //判断是否允许
            if (path.startsWith(allowPath)){
                return true;
            }
        }
        return false;
    }

    @Override
    public Object run() throws ZuulException {
        //获取上下文
        RequestContext ctx = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = ctx.getRequest();
        //获取token
        String token = CookieUtils.getCookieValue(request, prop.getCookieName());
        //解析token
        try {
            UserInfo user = JwtUtils.getInfoFromToken(token, prop.getPublicKey());
            //TODO 校验权限
        }catch (Exception e){
            //解析token失败,未登录,拦截
            ctx.setSendZuulResponse(false);
            //返回状态码
            ctx.setResponseStatusCode(403);
        }
            return null;
    }
}
