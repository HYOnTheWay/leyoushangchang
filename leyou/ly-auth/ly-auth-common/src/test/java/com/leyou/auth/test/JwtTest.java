package com.leyou.auth.test;

import com.leyou.pojo.UserInfo;
import com.leyou.utils.JwtUtils;
import com.leyou.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;

public class JwtTest {

    private static final String pubKeyPath = "F:\\视频\\rsa\\rsa.pub";

    private static final String priKeyPath = "F:\\视频\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        // 生成token
        String token = JwtUtils.generateToken(new UserInfo(20L, "jack"), privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6MjAsInVzZXJuYW1lIjoiamFjayIsImV4cCI6MTU2NzA0NDExMn0.JQ49hS2weV--OFLoYp1f5j45WPoltAZKuy5wemHKn1hHSFr5ctRyB_5lH-RSCxva4wfqtRKVNMCzqnlS4tW3b1hhoZq2ZDnegq1M2y_5Mfk8OR2tt4H1QbLwvKWotcqw0P5a2aMIAIxDAMCyuJcHA7p8g4duv8Ql0JJa8jMjb2M";

        // 解析token
        UserInfo user = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + user.getId());
        System.out.println("userName: " + user.getUsername());
    }
}