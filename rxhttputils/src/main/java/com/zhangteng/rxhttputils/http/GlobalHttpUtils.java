package com.zhangteng.rxhttputils.http;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.zhangteng.rxhttputils.config.EncryptConfig;
import com.zhangteng.rxhttputils.interceptor.AddCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.CacheInterceptor;
import com.zhangteng.rxhttputils.interceptor.CallBackInterceptor;
import com.zhangteng.rxhttputils.interceptor.DecryptionInterceptor;
import com.zhangteng.rxhttputils.interceptor.EncryptionInterceptor;
import com.zhangteng.rxhttputils.interceptor.HeaderInterceptor;
import com.zhangteng.rxhttputils.interceptor.HttpLoggingProxyInterceptor;
import com.zhangteng.rxhttputils.interceptor.PriorityInterceptor;
import com.zhangteng.rxhttputils.interceptor.SaveCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.SignInterceptor;
import com.zhangteng.rxhttputils.utils.RetrofitServiceProxyHandler;
import com.zhangteng.utils.FileUtilsKt;
import com.zhangteng.utils.LruCache;
import com.zhangteng.utils.SSLUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import okhttp3.Cache;
import okhttp3.Dns;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swing on 2018/4/24.
 */
public class GlobalHttpUtils {
    private static GlobalHttpUtils instance;

    /**
     * description: 全局okhttpBuilder，保证使用一个网络实例
     */
    private final okhttp3.OkHttpClient.Builder okhttpBuilder;
    /**
     * description: 全局retrofitBuilder，保证使用一个网络实例
     */
    private final Retrofit.Builder retrofitBuilder;
    /**
     * description: 全局okHttpClient，保证使用一个网络实例
     */
    private okhttp3.OkHttpClient okHttpClient;
    /**
     * description: 全局retrofit，保证使用一个网络实例
     */
    private Retrofit retrofit;

    /**
     * description: 最大缓存数量
     */
    private static final int MAX_SIZE = 150;
    /**
     * description: 缓存扩容因数
     */
    private static final float MAX_SIZE_MULTIPLIER = 0.002f;
    /**
     * description: 缓存数量
     */
    private static int cache_size = MAX_SIZE;
    /**
     * description: Lru缓存
     */
    private LruCache<String, Object> mRetrofitServiceCache;
    /**
     * description: 拦截器集合,按照优先级从小到大排序
     */
    private final TreeSet<PriorityInterceptor> priorityInterceptors;
    /**
     * description: 网络拦截器集合,按照优先级从小到大排序
     */
    private final TreeSet<PriorityInterceptor> networkInterceptors;

    private GlobalHttpUtils() {
        okhttpBuilder = new okhttp3.OkHttpClient.Builder();
        retrofitBuilder = new Retrofit.Builder();
        priorityInterceptors = new TreeSet<>((o, r) -> Integer.compare(o.getPriority(), r.getPriority()));
        networkInterceptors = new TreeSet<>((o, r) -> Integer.compare(o.getPriority(), r.getPriority()));
    }

    public static GlobalHttpUtils getInstance() {
        if (instance == null) {
            synchronized (GlobalHttpUtils.class) {
                if (instance == null) {
                    instance = new GlobalHttpUtils();
                }
            }
        }
        return instance;
    }

    /**
     * description 设置网络baseUrl
     *
     * @param baseUrl 接口前缀
     */
    public GlobalHttpUtils setBaseUrl(String baseUrl) {
        retrofitBuilder.baseUrl(baseUrl);
        return this;
    }

    /**
     * description 设置Converter.Factory,默认GsonConverterFactory.create()
     */
    public GlobalHttpUtils addConverterFactory(Converter.Factory factory) {
        retrofitBuilder.addConverterFactory(factory);
        return this;
    }

    /**
     * description 设置CallAdapter.Factory,默认RxJava2CallAdapterFactory.create()
     */
    public GlobalHttpUtils addCallAdapterFactory(CallAdapter.Factory factory) {
        retrofitBuilder.addCallAdapterFactory(factory);
        return this;
    }

    /**
     * description 设置域名解析服务器
     *
     * @param dns 域名解析服务器
     */
    public GlobalHttpUtils setDns(Dns dns) {
        okhttpBuilder.dns(dns);
        return this;
    }

    /**
     * description 添加单个请求头公共参数，当okHttpClient构建完成后依旧可以新增全局请求头参数，可随时添加修改公共请求头
     *
     * @param key   请求头 key
     * @param value 请求头 value
     */
    public GlobalHttpUtils addHeader(String key, Object value) {
        List<Interceptor> interceptors = okhttpBuilder.interceptors();
        Interceptor headerInterceptor = null;
        for (Interceptor interceptor : interceptors) {
            if (interceptor instanceof HeaderInterceptor) {
                headerInterceptor = interceptor;
                ((HeaderInterceptor) headerInterceptor).getHeaderMaps().put(key, value);
            }
        }
        if (headerInterceptor == null) {
            Map<String, Object> headerMaps = new HashMap<>();
            headerMaps.put(key, value);
            priorityInterceptors.add(new HeaderInterceptor(headerMaps));
        }
        return this;
    }

    /**
     * description 设置请求头公共参数，当okHttpClient构建完成后无法新增全局请求头参数
     *
     * @param headerMaps 请求头设置的静态参数
     */
    public GlobalHttpUtils setHeaders(Map<String, Object> headerMaps) {
        priorityInterceptors.add(new HeaderInterceptor(headerMaps));
        return this;
    }

    /**
     * description 动态设置请求头，如token等需要根据登录状态实时变化的请求头参数，最小支持api 24
     *
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public GlobalHttpUtils setHeaders(Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        priorityInterceptors.add(new HeaderInterceptor(headersFunction));
        return this;
    }

    /**
     * description 设置请求头公共参数，最小支持api 24
     *
     * @param headerMaps      请求头设置的静态参数
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public GlobalHttpUtils setHeaders(Map<String, Object> headerMaps, Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        priorityInterceptors.add(new HeaderInterceptor(headerMaps, headersFunction));
        return this;
    }

    /**
     * description 开启网络日志
     *
     * @param isShowLog 是否
     */
    public GlobalHttpUtils setLog(boolean isShowLog) {
        if (isShowLog) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Log.i("HttpUtils", message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            HttpLoggingProxyInterceptor proxyInterceptor = new HttpLoggingProxyInterceptor(loggingInterceptor);
            priorityInterceptors.add(proxyInterceptor);
        }
        return this;
    }

    /**
     * description 开启网络日志
     *
     * @param logger 自定义日志打印类
     */
    public GlobalHttpUtils setLog(HttpLoggingInterceptor.Logger logger) {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(logger);
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        HttpLoggingProxyInterceptor proxyInterceptor = new HttpLoggingProxyInterceptor(loggingInterceptor);
        priorityInterceptors.add(proxyInterceptor);
        return this;
    }

    /**
     * description 设置网络缓存，有网时使用网络默认缓存策略，无网时使用强制缓存策略，默认缓存文件路径Environment.getExternalStorageDirectory() + "/RxHttpUtilsCache"，缓存文件大小1024 * 1024
     *
     * @param isCache 是否开启缓存
     */
    public GlobalHttpUtils setCache(boolean isCache) {
        if (isCache) {
            CacheInterceptor cacheInterceptor = new CacheInterceptor();
            File file = new File(FileUtilsKt.getDiskCacheDir(HttpUtils.getInstance().getContext()) + "/RxHttpUtilsCache");
            Cache cache = new Cache(file, 1024 * 1024);
            priorityInterceptors.add(cacheInterceptor);
            networkInterceptors.add(cacheInterceptor);
            okhttpBuilder.cache(cache);
        }
        return this;
    }

    /**
     * description 设置网络缓存，有网时使用网络默认缓存策略，无网时使用强制缓存策略
     *
     * @param isCache 是否开启缓存
     * @param path    缓存文件路径
     * @param maxSize 缓存文件大小
     */
    public GlobalHttpUtils setCache(boolean isCache, String path, long maxSize) {
        if (isCache) {
            CacheInterceptor cacheInterceptor = new CacheInterceptor();
            File file = new File(path);
            Cache cache = new Cache(file, maxSize);
            priorityInterceptors.add(cacheInterceptor);
            networkInterceptors.add(cacheInterceptor);
            okhttpBuilder.cache(cache);
        }
        return this;
    }

    /**
     * description 设置Cookie
     *
     * @param saveCookie 是否设置Cookie
     */
    public GlobalHttpUtils setCookie(boolean saveCookie) {
        if (saveCookie) {
            priorityInterceptors.add(new AddCookieInterceptor());
            networkInterceptors.add(new SaveCookieInterceptor());
        }
        return this;
    }

    /**
     * description 设置网络请求前后回调函数
     *
     * @param callBack 网络回调类
     */
    public GlobalHttpUtils setHttpCallBack(CallBackInterceptor.CallBack callBack) {
        if (callBack != null) {
            priorityInterceptors.add(new CallBackInterceptor(callBack));
        }
        return this;
    }

    /**
     * description 网络请求加签
     * 1、身份验证：是否是我规定的那个人
     * 2、防篡改：是否被第三方劫持并篡改参数
     * 3、防重放：是否重复请求
     *
     * @param appKey 验签时前后端匹配的appKey，前后端一致即可
     */
    public GlobalHttpUtils setSign(String appKey) {
        priorityInterceptors.add(new SignInterceptor(appKey));
        return this;
    }

    /**
     * description 数据加解密
     * 数据加密，防止信息截取，具体加解密方案参考https://blog.csdn.net/duoluo9/article/details/105214983?spm=1001.2014.3001.5501
     *
     * @param publicKeyUrl rsa公钥失效后重新请求秘钥的接口
     * @param publicKey    rsa公钥
     */
    public GlobalHttpUtils setEnAndDecryption(HttpUrl publicKeyUrl, String publicKey) {
        EncryptConfig.publicKeyUrl = publicKeyUrl;
        EncryptConfig.publicKey = publicKey;
        priorityInterceptors.add(new EncryptionInterceptor());
        networkInterceptors.add(new DecryptionInterceptor());
        return this;
    }

    /**
     * description 添加拦截器
     *
     * @param interceptor 带优先级的拦截器
     */
    public GlobalHttpUtils addInterceptor(PriorityInterceptor interceptor) {
        priorityInterceptors.add(interceptor);
        return this;
    }

    /**
     * description 添加拦截器
     *
     * @param interceptors 带优先级的拦截器
     */
    public GlobalHttpUtils addInterceptors(List<PriorityInterceptor> interceptors) {
        priorityInterceptors.addAll(interceptors);
        return this;
    }

    /**
     * description 添加网络拦截器
     *
     * @param interceptor 带优先级的拦截器
     */
    public GlobalHttpUtils addNetworkInterceptor(PriorityInterceptor interceptor) {
        networkInterceptors.add(interceptor);
        return this;
    }

    /**
     * description 添加网络拦截器
     *
     * @param interceptors 带优先级的拦截器
     */
    public GlobalHttpUtils addNetworkInterceptors(List<PriorityInterceptor> interceptors) {
        networkInterceptors.addAll(interceptors);
        return this;
    }

    /**
     * description 超时时间
     *
     * @param second 秒
     */
    public GlobalHttpUtils setReadTimeOut(long second) {
        okhttpBuilder
                .readTimeout(second, TimeUnit.SECONDS);
        return this;
    }

    /**
     * description 超时时间
     *
     * @param second 秒
     */
    public GlobalHttpUtils setWriteTimeOut(long second) {
        okhttpBuilder
                .writeTimeout(second, TimeUnit.SECONDS);
        return this;
    }

    /**
     * description 超时时间
     *
     * @param second 秒
     */
    public GlobalHttpUtils setConnectionTimeOut(long second) {
        okhttpBuilder
                .connectTimeout(second, TimeUnit.SECONDS);
        return this;
    }

    /**
     * description 信任所有证书,不安全有风险
     */
    public GlobalHttpUtils setSslSocketFactory() {
        SSLUtils.SSLParams sslParams = SSLUtils.INSTANCE.getSslSocketFactory();
        okhttpBuilder.sslSocketFactory(Objects.requireNonNull(sslParams.getSSLSocketFactory()), Objects.requireNonNull(sslParams.getTrustManager()));
        return this;
    }

    /**
     * description 使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param certificates 证书
     */
    public GlobalHttpUtils setSslSocketFactory(InputStream... certificates) {
        SSLUtils.SSLParams sslParams = SSLUtils.INSTANCE.getSslSocketFactory(certificates);
        okhttpBuilder.sslSocketFactory(Objects.requireNonNull(sslParams.getSSLSocketFactory()), Objects.requireNonNull(sslParams.getTrustManager()));
        return this;
    }

    /**
     * description 使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param bksFile      bks证书
     * @param password     密码
     * @param certificates 证书
     */
    public GlobalHttpUtils setSslSocketFactory(InputStream bksFile, String password, InputStream... certificates) {
        SSLUtils.SSLParams sslParams = SSLUtils.INSTANCE.getSslSocketFactory(bksFile, password, certificates);
        okhttpBuilder.sslSocketFactory(Objects.requireNonNull(sslParams.getSSLSocketFactory()), Objects.requireNonNull(sslParams.getTrustManager()));
        return this;
    }

    /**
     * description 获取RetrofitService，如果已被创建则添加缓存，下次直接从缓存中获取RetrofitService
     *
     * @param cls 网络接口
     */
    public <K> K createService(Class<K> cls) {
        if (mRetrofitServiceCache == null) {
            try {
                ActivityManager activityManager = (ActivityManager) HttpUtils.getInstance().getContext().getSystemService(Context.ACTIVITY_SERVICE);
                int targetMemoryCacheSize = (int) (activityManager.getMemoryClass() * MAX_SIZE_MULTIPLIER * 1024);
                if (targetMemoryCacheSize < MAX_SIZE) {
                    cache_size = targetMemoryCacheSize;
                }
            } catch (ExceptionInInitializerError exception) {
                cache_size = MAX_SIZE;
            }
            mRetrofitServiceCache = new LruCache<>(cache_size);
        }
        K retrofitService = (K) mRetrofitServiceCache.get(cls.getCanonicalName());
        if (retrofitService == null) {
            retrofitService = (K) Proxy.newProxyInstance(
                    cls.getClassLoader(),
                    new Class[]{cls},
                    new RetrofitServiceProxyHandler(getRetrofit(), cls));
            mRetrofitServiceCache.put(cls.getCanonicalName(), retrofitService);
        }
        return retrofitService;
    }

    /**
     * description 动态代理方式获取RetrofitService
     *
     * @param cls 网络接口
     */
    public <K> K createServiceNoCache(Class<K> cls) {
        return (K) Proxy.newProxyInstance(
                cls.getClassLoader(),
                new Class[]{cls},
                new RetrofitServiceProxyHandler(getRetrofit(), cls));
    }

    /**
     * description 全局 okhttpBuilder
     */
    public okhttp3.OkHttpClient.Builder getOkHttpClientBuilder() {
        return okhttpBuilder;
    }

    /**
     * description 全局 retrofitBuilder
     */
    public Retrofit.Builder getRetrofitBuilder() {
        return retrofitBuilder;
    }

    /**
     * description 全局 okHttpClient，当okHttpClient构建完成后无法新增全局参数
     */
    public okhttp3.OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            for (PriorityInterceptor priorityInterceptor : priorityInterceptors) {
                okhttpBuilder.addInterceptor(priorityInterceptor);
            }
            for (PriorityInterceptor priorityInterceptor : networkInterceptors) {
                okhttpBuilder.addNetworkInterceptor(priorityInterceptor);
            }
            okHttpClient = okhttpBuilder.build();
        }
        return okHttpClient;
    }

    /**
     * description 全局 retrofit，当retrofit构建完成后无法修改全局baseUrl
     */
    public Retrofit getRetrofit() {
        if (okHttpClient == null) {
            for (PriorityInterceptor priorityInterceptor : priorityInterceptors) {
                okhttpBuilder.addInterceptor(priorityInterceptor);
            }
            for (PriorityInterceptor priorityInterceptor : networkInterceptors) {
                okhttpBuilder.addNetworkInterceptor(priorityInterceptor);
            }
            okHttpClient = okhttpBuilder.build();
        }
        if (retrofit == null) {
            if (retrofitBuilder.callAdapterFactories().isEmpty()) {
                retrofitBuilder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
            }
            if (retrofitBuilder.converterFactories().isEmpty()) {
                retrofitBuilder.addConverterFactory(GsonConverterFactory.create());
            }
            retrofit = retrofitBuilder.client(okHttpClient).build();
        }
        return retrofit;
    }
}
