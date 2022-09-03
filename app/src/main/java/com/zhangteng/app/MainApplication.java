package com.zhangteng.app;

import android.app.Application;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.interceptor.CallBackInterceptor;
import com.zhangteng.utils.LogUtilsKt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainApplication extends Application {
    private static MainApplication mainApplication;
    private final Map<String, Object> headersMap = new HashMap<>();

    public static MainApplication getInstance() {
        return mainApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mainApplication = this;
        HttpUtils.init(this);
        //全局网络配置
        HttpUtils.getInstance()
                .ConfigGlobalHttpUtils()
                //全局的BaseUrl
                .setBaseUrl("https://www.baidu.com")
                //设置CallAdapter.Factory,默认RxJavaCallAdapterFactory.create()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                //设置Converter.Factory,默认GsonConverterFactory.create()
                .addConverterFactory(GsonConverterFactory.create())
                //设置自定义域名解析
                .setDns(HttpDns.getInstance())
                //开启缓存策略
                .setCache(true)
                //全局的单个请求头信息
                .addHeader("Authorization", "Bearer ")
                //全局的静态请求头信息
                .setHeaders(headersMap)
                //全局的请求头信息
//                .setHeaders(headersMap, headers -> {
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
//                .setHeaders(headers -> {
//                    if (headers == null) {
//                        headers = new HashMap<>();
//                    }
//                    headers.put("version", BuildConfig.VERSION_CODE);
//                    headers.put("os", "android");
//
//                    boolean isLogin = BuildConfig.DEBUG;
//                    if (isLogin) {
//                        headers.put("Authorization", "Bearer " + "token");
//                    } else {
//                        headers.remove("Authorization");
//                    }
//                    return headers;
//                })
                //全局持久话cookie,保存本地每次都会携带在header中
                .setCookie(false)
                .setHttpCallBack(new CallBackInterceptor.CallBack() {
                    @NonNull
                    @Override
                    public Response onHttpResponse(@NonNull Interceptor.Chain chain, @NonNull Response response) {
                        //这里可以先客户端一步拿到每一次 Http 请求的结果
                        ResponseBody body = response.newBuilder().build().body();
                        BufferedSource source = body.source();
                        try {
                            source.request(Long.MAX_VALUE); // Buffer the entire body.
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Buffer buffer = source.getBuffer();
                        Charset charset = StandardCharsets.UTF_8;
                        MediaType contentType = body.contentType();
                        if (contentType != null) {
                            charset = contentType.charset(charset);
                        }
                        LogUtilsKt.e(buffer.readString(charset));
                        return response;
                    }

                    @NonNull
                    @Override
                    public Request onHttpRequest(@NonNull Interceptor.Chain chain, @NonNull Request request) {
                        //这里可以在请求服务器之前拿到
                        LogUtilsKt.e(new Gson().toJson(request.headers()));
                        RequestBody body = request.body();
                        if (body != null) {
                            try {
                                LogUtilsKt.e(String.valueOf(body.contentLength()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        return request;
                    }
                })
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
