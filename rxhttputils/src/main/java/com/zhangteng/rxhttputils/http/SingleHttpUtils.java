package com.zhangteng.rxhttputils.http;

import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.zhangteng.rxhttputils.config.EncryptConfig;
import com.zhangteng.rxhttputils.interceptor.AddCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.CacheInterceptor;
import com.zhangteng.rxhttputils.interceptor.DecryptionInterceptor;
import com.zhangteng.rxhttputils.interceptor.EncryptionInterceptor;
import com.zhangteng.rxhttputils.interceptor.HeaderInterceptor;
import com.zhangteng.rxhttputils.interceptor.SaveCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.SignInterceptor;
import com.zhangteng.rxhttputils.utils.RetrofitServiceProxyHandler;
import com.zhangteng.utils.SSLUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;

/**
 * Created by swing on 2018/4/24.
 */
public class SingleHttpUtils {
    private static SingleHttpUtils instance;
    private String baseUrl;

    private Map<String, Object> headerMaps = new HashMap<>();
    private Function<Map<String, Object>, Map<String, Object>> headersFunction;

    private boolean isShowLog = true;
    private HttpLoggingInterceptor.Logger logger = null;
    private boolean isCache = false;
    private boolean saveCookie = true;
    private boolean sign = false;
    private boolean encrypt = false;

    private String cachePath;
    private String appKey;
    private long cacheMaxSize;
    private HttpUrl publicKeyUrl;
    private String publicKey;

    private long readTimeout;
    private long writeTimeout;
    private long connectTimeout;

    private SSLUtils.SSLParams sslParams;

    private final List<Converter.Factory> converterFactories = new ArrayList<>();
    private final List<CallAdapter.Factory> adapterFactories = new ArrayList<>();

    public static SingleHttpUtils getInstance() {
        instance = new SingleHttpUtils();
        return instance;
    }

    /**
     * description 设置网络baseUrl
     *
     * @param baseUrl 接口前缀
     */
    public SingleHttpUtils setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * 局部设置Converter.Factory,默认GsonConverterFactory.create()
     */
    public SingleHttpUtils addConverterFactory(Converter.Factory factory) {
        if (factory != null) {
            converterFactories.add(factory);
        }
        return this;
    }

    /**
     * 局部设置CallAdapter.Factory,默认RxJavaCallAdapterFactory.create()
     */
    public SingleHttpUtils addCallAdapterFactory(CallAdapter.Factory factory) {
        if (factory != null) {
            adapterFactories.add(factory);
        }
        return this;
    }

    public SingleHttpUtils setHeaders(Map<String, Object> headerMaps) {
        this.headerMaps = headerMaps;
        return this;
    }

    /**
     * description 动态设置请求头，如token等需要根据登录状态实时变化的请求头参数，最小支持api 24
     *
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public SingleHttpUtils setHeaders(Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        this.headersFunction = headersFunction;
        return this;
    }

    /**
     * description 设置请求头公共参数，最小支持api 24
     *
     * @param headerMaps      请求头设置的静态参数
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public SingleHttpUtils setHeaders(Map<String, Object> headerMaps, Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        this.headerMaps = headerMaps;
        this.headersFunction = headersFunction;
        return this;
    }

    /**
     * description 开启网络日志
     *
     * @param isShowLog 是否
     */
    public SingleHttpUtils setLog(boolean isShowLog) {
        this.isShowLog = isShowLog;
        return this;
    }

    /**
     * description 开启网络日志
     *
     * @param logger 自定义日志打印类
     */
    public SingleHttpUtils setLog(HttpLoggingInterceptor.Logger logger) {
        this.isShowLog = true;
        this.logger = logger;
        return this;
    }

    /**
     * description 设置网络缓存，有网时使用网络默认缓存策略，无网时使用强制缓存策略，默认缓存文件路径Environment.getExternalStorageDirectory() + "/RxHttpUtilsCache"，缓存文件大小1024 * 1024
     *
     * @param isCache 是否开启缓存
     */
    public SingleHttpUtils setCache(boolean isCache) {
        this.isCache = isCache;
        return this;
    }

    /**
     * description 设置网络缓存，有网时使用网络默认缓存策略，无网时使用强制缓存策略
     *
     * @param isCache   是否开启缓存
     * @param cachePath 缓存文件路径
     * @param maxSize   缓存文件大小
     */
    public SingleHttpUtils setCache(boolean isCache, String cachePath, long maxSize) {
        this.isCache = isCache;
        this.cachePath = cachePath;
        this.cacheMaxSize = maxSize;
        return this;
    }

    /**
     * description 设置Cookie
     *
     * @param saveCookie 是否设置Cookie
     */
    public SingleHttpUtils setCookie(boolean saveCookie) {
        this.saveCookie = saveCookie;
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
    public SingleHttpUtils setSign(String appKey) {
        this.sign = true;
        this.appKey = appKey;
        return this;
    }

    /**
     * description 数据加解密
     * 数据加密，防止信息截取，具体加解密方案参考https://blog.csdn.net/duoluo9/article/details/105214983?spm=1001.2014.3001.5501
     *
     * @param publicKeyUrl rsa公钥失效后重新请求秘钥的接口
     * @param publicKey    rsa公钥
     */
    public SingleHttpUtils setEnAndDecryption(HttpUrl publicKeyUrl, String publicKey) {
        this.encrypt = true;
        this.publicKey = publicKey;
        this.publicKeyUrl = publicKeyUrl;
        return this;
    }

    /**
     * description 超时时间
     *
     * @param readTimeout 秒
     */
    public SingleHttpUtils setReadTimeOut(long readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * description 超时时间
     *
     * @param writeTimeout 秒
     */
    public SingleHttpUtils setWriteTimeOut(long writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    /**
     * description 超时时间
     *
     * @param connectTimeout 秒
     */
    public SingleHttpUtils setConnectionTimeOut(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * description 信任所有证书,不安全有风险
     */
    public SingleHttpUtils setSslSocketFactory() {
        sslParams = SSLUtils.INSTANCE.getSslSocketFactory();
        return this;
    }

    /**
     * description 使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param certificates 证书
     */
    public SingleHttpUtils setSslSocketFactory(InputStream... certificates) {
        sslParams = SSLUtils.INSTANCE.getSslSocketFactory(certificates);
        return this;
    }

    /**
     * description 使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param bksFile      bks证书
     * @param password     密码
     * @param certificates 证书
     */
    public SingleHttpUtils setSslSocketFactory(InputStream bksFile, String password, InputStream... certificates) {
        sslParams = SSLUtils.INSTANCE.getSslSocketFactory(bksFile, password, certificates);
        return this;
    }


    /**
     * description 动态代理方式获取RetrofitService
     *
     * @param cls 网络接口
     */
    public <K> K createService(Class<K> cls) {
        return (K) Proxy.newProxyInstance(
                cls.getClassLoader(),
                new Class[]{cls},
                new RetrofitServiceProxyHandler(getSingleRetrofitBuilder().build(), cls));
    }


    /**
     * description 单个RetrofitBuilder
     */
    private Retrofit.Builder getSingleRetrofitBuilder() {

        Retrofit.Builder singleRetrofitBuilder = new Retrofit.Builder();

        if (converterFactories.isEmpty()) {
            //获取全局的对象重新设置
            List<Converter.Factory> listConverterFactory = RetrofitClient.getInstance().getRetrofit().converterFactories();
            for (Converter.Factory factory : listConverterFactory) {
                singleRetrofitBuilder.addConverterFactory(factory);
            }
        } else {
            for (Converter.Factory converterFactory : converterFactories) {
                singleRetrofitBuilder.addConverterFactory(converterFactory);
            }
        }

        if (adapterFactories.isEmpty()) {
            //获取全局的对象重新设置
            List<CallAdapter.Factory> listAdapterFactory = RetrofitClient.getInstance().getRetrofit().callAdapterFactories();
            for (CallAdapter.Factory factory : listAdapterFactory) {
                singleRetrofitBuilder.addCallAdapterFactory(factory);
            }

        } else {
            for (CallAdapter.Factory adapterFactory : adapterFactories) {
                singleRetrofitBuilder.addCallAdapterFactory(adapterFactory);
            }
        }


        if (baseUrl == null || baseUrl.isEmpty()) {
            singleRetrofitBuilder.baseUrl(RetrofitClient.getInstance().getRetrofit().baseUrl());
        } else {
            singleRetrofitBuilder.baseUrl(baseUrl);
        }

        singleRetrofitBuilder.client(getSingleOkHttpBuilder().build());

        return singleRetrofitBuilder;
    }

    /**
     * description 获取单个 OkHttpClient.Builder
     */
    private OkHttpClient.Builder getSingleOkHttpBuilder() {

        OkHttpClient.Builder singleOkHttpBuilder = new OkHttpClient.Builder();

        singleOkHttpBuilder.retryOnConnectionFailure(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            singleOkHttpBuilder.addInterceptor(new HeaderInterceptor(headerMaps, headersFunction));
        } else {
            singleOkHttpBuilder.addInterceptor(new HeaderInterceptor(headerMaps));
        }

        if (sign) {
            singleOkHttpBuilder.addInterceptor(new SignInterceptor(appKey));
        }
        if (isCache) {
            CacheInterceptor cacheInterceptor = new CacheInterceptor();
            Cache cache;
            if ((cachePath != null && !cachePath.isEmpty()) && cacheMaxSize > 0) {
                cache = new Cache(new File(cachePath), cacheMaxSize);
            } else {
                cache = new Cache(new File(Environment.getExternalStorageDirectory().getPath() + "/RxHttpUtilsCache")
                        , 1024 * 1024);
            }
            singleOkHttpBuilder.addInterceptor(cacheInterceptor)
                    .addNetworkInterceptor(cacheInterceptor)
                    .cache(cache);
        }
        if (isShowLog) {
            HttpLoggingInterceptor loggingInterceptor;
            if (logger == null) {
                loggingInterceptor = new HttpLoggingInterceptor(message -> Log.e("HttpUtils", message));
            } else {
                loggingInterceptor = new HttpLoggingInterceptor(logger);
            }
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            singleOkHttpBuilder.addInterceptor(loggingInterceptor);
        }

        if (saveCookie) {
            singleOkHttpBuilder
                    .addInterceptor(new AddCookieInterceptor())
                    .addInterceptor(new SaveCookieInterceptor());
        }
        if (encrypt) {
            EncryptConfig.publicKey = publicKey;
            EncryptConfig.publicKeyUrl = publicKeyUrl;
            singleOkHttpBuilder.addInterceptor(new EncryptionInterceptor());
            singleOkHttpBuilder.addNetworkInterceptor(new DecryptionInterceptor());
        }
        singleOkHttpBuilder.readTimeout(readTimeout > 0 ? readTimeout : 10, TimeUnit.SECONDS);

        singleOkHttpBuilder.writeTimeout(writeTimeout > 0 ? writeTimeout : 10, TimeUnit.SECONDS);

        singleOkHttpBuilder.connectTimeout(connectTimeout > 0 ? connectTimeout : 10, TimeUnit.SECONDS);

        if (sslParams != null) {
            singleOkHttpBuilder.sslSocketFactory(Objects.requireNonNull(sslParams.getSSLSocketFactory()), Objects.requireNonNull(sslParams.getTrustManager()));
        }

        return singleOkHttpBuilder;
    }
}
