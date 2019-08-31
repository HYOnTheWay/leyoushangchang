package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.common.enums.ExceptionEnum;
import com.leyou.common.exception.LyException;
import com.leyou.pojo.UserInfo;
import com.leyou.user.pojo.User;
import com.leyou.utils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserClient userClient;

    public String login(String username, String password) {
        try {
            //校验用户名和密码
            User user = userClient.queryUserByUsernameAndPassword(username, password);
            if (user == null) {
                throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
            }
            //生成token
            String token = JwtUtils.generateToken(new UserInfo(user.getId(), username), jwtProperties.getPrivateKey(), jwtProperties.getExpire());
            return token;
        }catch (Exception e){
            log.error("[授权中心] 用户名或者密码有误,用户名:{}",username,e);
            throw new LyException(ExceptionEnum.INVALID_USERNAME_PASSWORD);
        }
    }
}
