package com.zhangteng.rxhttputils.config;

import okhttp3.HttpUrl;

public class EncryptConfig {
    /**
     * 加密时请求头中放置加密后的AES秘钥的key
     */
    public static String SECRET = "_secret";
    /**
     * 加解密失败时返回的异常状态码，返回数据结构{"message": "移动端加密失败","status": SECRET_ERROR}
     */
    public static int SECRET_ERROR = 2100;
    public static String publicKey = "";
    public static HttpUrl publicKeyUrl;
}
