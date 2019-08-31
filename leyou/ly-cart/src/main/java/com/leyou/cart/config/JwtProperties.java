package com.leyou.cart.config;

import com.leyou.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@ConfigurationProperties(prefix = "ly.jwt")
@Data
public class JwtProperties {

    private String pubKeyPath;

    private String cookieName;

    private PublicKey publicKey;// 公钥

    //对象一旦实例化后,就应该读取公钥和私钥
    @PostConstruct
    public void init() throws Exception {
        //读取公钥和私钥
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
    }
}
