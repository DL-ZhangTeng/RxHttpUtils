package com.zhangteng.app;

import android.app.Application;

import com.zhangteng.rxhttputils.http.HttpUtils;

import java.util.HashMap;

import okhttp3.OkHttpClient;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        HttpUtils.init(this);
        //全局网络配置
        HttpUtils.getInstance()
                .ConfigGlobalHttpUtils()
                //全局的BaseUrl
                .setBaseUrl("https://**/")
                //开启缓存策略
                .setCache(true)
                //全局的静态请求头信息
//                .setHeaders(headers)
                //全局的请求头信息
//                .setHeaders(headers, headers -> {
//                    if (headers == null) {
//                        headers = new HashMap<>();
//                    }
//                    boolean isLogin = BuildConfig.DEBUG;
//                    if (isLogin) {
//                        headers.put("Authorization", "Bearer " + "token");
//                    } else {
//                        headers.remove("Authorization");
//                    }
//                    return headers;
//                })
                //全局的动态请求头信息
                .setHeaders(headers -> {
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    headers.put("version", BuildConfig.VERSION_CODE);
                    headers.put("os", "android");

                    boolean isLogin = BuildConfig.DEBUG;
                    if (isLogin) {
                        headers.put("Authorization", "Bearer " + "token");
                    } else {
                        headers.remove("Authorization");
                    }
                    return headers;
                })
                //全局持久话cookie,保存本地每次都会携带在header中
                .setCookie(false)
                //全局ssl证书认证
                //信任所有证书,不安全有风险
                .setSslSocketFactory()
                //使用预埋证书，校验服务端证书（自签名证书）
                //.setSslSocketFactory(getAssets().open("your.cer"))
                //使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
                //.setSslSocketFactory(getAssets().open("your.bks"), "123456", getAssets().open("your.cer"))
                //全局超时配置
                .setReadTimeOut(10)
                //全局超时配置
                .setWriteTimeOut(10)
                //全局超时配置
                .setConnectionTimeOut(10)
                //全局是否打开请求log日志
                .setLog(true);
        //上传配置，默认使用全局配置可不设置（如token等信息全局配置后上传文件可通过验证token）
        HttpUtils.getInstance()
                .UploadRetrofit()
                .setBaseUrl("http://**/")
                .setOkHttpClient(new OkHttpClient());
        //下载配置，默认使用全局配置可不设置（如token等信息全局配置后下载文件可通过验证token）
        HttpUtils.getInstance()
                .DownloadRetrofit()
                .setBaseUrl("http://**/")
                .setOkHttpClient(new OkHttpClient());
    }
}
