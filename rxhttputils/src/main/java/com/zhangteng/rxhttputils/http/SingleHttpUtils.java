package com.zhangteng.rxhttputils.http;

import android.os.Environment;
import android.util.Log;

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

    public SingleHttpUtils setLog(boolean isShowLog) {
        this.isShowLog = isShowLog;
        return this;
    }

    public SingleHttpUtils setLog(HttpLoggingInterceptor.Logger logger) {
        this.isShowLog = true;
        this.logger = logger;
        return this;
    }

    public SingleHttpUtils setCache(boolean isCache) {
        this.isCache = isCache;
        return this;
    }

    public SingleHttpUtils setCache(boolean isCache, String cachePath, long maxSize) {
        this.isCache = isCache;
        this.cachePath = cachePath;
        this.cacheMaxSize = maxSize;
        return this;
    }

    public SingleHttpUtils setCookie(boolean saveCookie) {
        this.saveCookie = saveCookie;
        return this;
    }

    /**
     * @param appKey 验签时前后端匹配的appKey，前后端一致即可
     */
    public SingleHttpUtils setSign(String appKey) {
        this.sign = true;
        this.appKey = appKey;
        return this;
    }

    /**
     * @param publicKeyUrl rsa公钥失效后重新请求秘钥的接口
     * @param publicKey    rsa公钥
     */
    public SingleHttpUtils setEnAndDecryption(HttpUrl publicKeyUrl, String publicKey) {
        this.encrypt = true;
        this.publicKey = publicKey;
        this.publicKeyUrl = publicKeyUrl;
        return this;
    }

    public SingleHttpUtils setReadTimeOut(long readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public SingleHttpUtils setWriteTimeOut(long writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public SingleHttpUtils setConnectionTimeOut(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 信任所有证书,不安全有风险
     *
     * @return
     */
    public SingleHttpUtils setSslSocketFactory() {
        sslParams = SSLUtils.INSTANCE.getSslSocketFactory();
        return this;
    }

    /**
     * 使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param certificates
     * @return
     */
    public SingleHttpUtils setSslSocketFactory(InputStream... certificates) {
        sslParams = SSLUtils.INSTANCE.getSslSocketFactory(certificates);
        return this;
    }

    /**
     * 使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param bksFile
     * @param password
     * @param certificates
     * @return
     */
    public SingleHttpUtils setSslSocketFactory(InputStream bksFile, String password, InputStream... certificates) {
        sslParams = SSLUtils.INSTANCE.getSslSocketFactory(bksFile, password, certificates);
        return this;
    }


    /**
     * 使用自己自定义参数创建请求
     *
     * @param cls
     * @param <K>
     * @return
     */
    public <K> K createService(Class<K> cls) {
        return (K) Proxy.newProxyInstance(
                cls.getClassLoader(),
                new Class[]{cls},
                new RetrofitServiceProxyHandler(getSingleRetrofitBuilder().build(), cls));
    }


    /**
     * 单个RetrofitBuilder
     *
     * @return
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
     * 获取单个 OkHttpClient.Builder
     *
     * @return
     */
    private OkHttpClient.Builder getSingleOkHttpBuilder() {

        OkHttpClient.Builder singleOkHttpBuilder = new OkHttpClient.Builder();

        singleOkHttpBuilder.retryOnConnectionFailure(true);

        singleOkHttpBuilder.addInterceptor(new HeaderInterceptor(headerMaps));
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
