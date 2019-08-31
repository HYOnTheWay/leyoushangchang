package com.leyou.auth.web;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.service.AuthService;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.CookieUtils;
import com.leyou.pojo.UserInfo;
import com.leyou.utils.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private AuthService authService;

    /**
     * 登录授权功能
     * @param username
     * @param password
     * @return
     */
    @PostMapping("login")
    public ResponseEntity<Void> login(
            @RequestParam("username")String username, @RequestParam("password")String password,
            HttpServletRequest request,
            HttpServletResponse response){
        //登录
        String token = authService.login(username,password);
        //写入cookie
        CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,true);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("verify")
    public ResponseEntity<UserInfo> verify(
            @CookieValue("LY_TOKEN")String token,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            //解析token
            UserInfo info = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            //刷新token,重新生成
            String newToken = JwtUtils.generateToken(info, jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            //写回cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),newToken,true);
            return ResponseEntity.ok(info);
        }catch (Exception e){
            //token被篡改,或者过期
            throw new LyException(ExceptionEnum.UN_AUTHORIZED);
        }
    }

}
