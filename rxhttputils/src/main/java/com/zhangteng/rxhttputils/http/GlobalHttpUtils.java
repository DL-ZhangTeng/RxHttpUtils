package com.zhangteng.rxhttputils.http;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.util.LruCache;

import com.zhangteng.rxhttputils.config.EncryptConfig;
import com.zhangteng.rxhttputils.interceptor.AddCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.CacheInterceptor;
import com.zhangteng.rxhttputils.interceptor.DecryptionInterceptor;
import com.zhangteng.rxhttputils.interceptor.EncryptionInterceptor;
import com.zhangteng.rxhttputils.interceptor.HeaderInterceptor;
import com.zhangteng.rxhttputils.interceptor.SaveCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.SignInterceptor;
import com.zhangteng.rxhttputils.utils.RetrofitServiceProxyHandler;
import com.zhangteng.rxhttputils.utils.SSLUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

/**
 * Created by swing on 2018/4/24.
 */
public class GlobalHttpUtils {
    private static GlobalHttpUtils instance;
    private static final int MAX_SIZE = 150;
    private static final float MAX_SIZE_MULTIPLIER = 0.002f;
    private static int cache_size = MAX_SIZE;
    private LruCache<String, Object> mRetrofitServiceCache;

    private GlobalHttpUtils() {
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

    public GlobalHttpUtils setBaseUrl(String baseUrl) {
        getRetrofitBuilder().baseUrl(baseUrl);
        return this;
    }

    public GlobalHttpUtils setHeaders(Map<String, Object> headerMaps) {
        getOkHttpClientBuilder().addInterceptor(new HeaderInterceptor(headerMaps));
        return this;
    }

    public GlobalHttpUtils setLog(boolean isShowLog) {
        if (isShowLog) {
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> Log.i("GlobalHttpUtils", message));
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            getOkHttpClientBuilder().addInterceptor(loggingInterceptor);
        }
        return this;
    }

    public GlobalHttpUtils setCache() {
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        File file = new File(Environment.getExternalStorageDirectory() + "/RxHttpUtilsCache");
        Cache cache = new Cache(file, 1024 * 1024);
        getOkHttpClientBuilder()
                .addInterceptor(cacheInterceptor)
                .addNetworkInterceptor(cacheInterceptor)
                .cache(cache);
        return this;
    }

    public GlobalHttpUtils setCache(String path, long maxSize) {
        CacheInterceptor cacheInterceptor = new CacheInterceptor();
        File file = new File(path);
        Cache cache = new Cache(file, maxSize);
        getOkHttpClientBuilder()
                .addInterceptor(cacheInterceptor)
                .addNetworkInterceptor(cacheInterceptor)
                .cache(cache);
        return this;
    }

    public GlobalHttpUtils setCookie(boolean saveCookie) {
        if (saveCookie) {
            getOkHttpClientBuilder()
                    .addInterceptor(new AddCookieInterceptor())
                    .addNetworkInterceptor(new SaveCookieInterceptor());
        }
        return this;
    }

    /**
     * @param appKey 验签时前后端匹配的appKey，前后端一致即可
     */
    public GlobalHttpUtils setSign(String appKey) {
        getOkHttpClientBuilder().addInterceptor(new SignInterceptor(appKey));
        return this;
    }

    /**
     * @param publicKeyUrl rsa公钥失效后重新请求秘钥的接口
     * @param publicKey    rsa公钥
     */
    public GlobalHttpUtils setEnAndDecryption(HttpUrl publicKeyUrl, String publicKey) {
        EncryptConfig.publicKeyUrl = publicKeyUrl;
        EncryptConfig.publicKey = publicKey;
        getOkHttpClientBuilder().addInterceptor(new EncryptionInterceptor());
        getOkHttpClientBuilder().addNetworkInterceptor(new DecryptionInterceptor());
        return this;
    }

    public GlobalHttpUtils setReadTimeOut(long second) {
        getOkHttpClientBuilder()
                .readTimeout(second, TimeUnit.SECONDS);
        return this;
    }

    public GlobalHttpUtils setWriteTimeOut(long second) {
        getOkHttpClientBuilder()
                .writeTimeout(second, TimeUnit.SECONDS);
        return this;
    }

    public GlobalHttpUtils setConnectionTimeOut(long second) {
        getOkHttpClientBuilder()
                .connectTimeout(second, TimeUnit.SECONDS);
        return this;
    }

    /**
     * 信任所有证书,不安全有风险
     *
     * @return
     */
    public GlobalHttpUtils setSslSocketFactory() {
        SSLUtils.SSLParams sslParams = SSLUtils.getSslSocketFactory();
        getOkHttpClientBuilder().sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        return this;
    }

    /**
     * 使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param certificates
     * @return
     */
    public GlobalHttpUtils setSslSocketFactory(InputStream... certificates) {
        SSLUtils.SSLParams sslParams = SSLUtils.getSslSocketFactory(certificates);
        getOkHttpClientBuilder().sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
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
    public GlobalHttpUtils setSslSocketFactory(InputStream bksFile, String password, InputStream... certificates) {
        SSLUtils.SSLParams sslParams = SSLUtils.getSslSocketFactory(bksFile, password, certificates);
        getOkHttpClientBuilder().sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        return this;
    }

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

    public okhttp3.OkHttpClient.Builder getOkHttpClientBuilder() {
        return OkHttpClient.getInstance().getBuilder();
    }

    public okhttp3.OkHttpClient getOkHttpClient() {
        return OkHttpClient.getInstance().getClient();
    }

    public Retrofit.Builder getRetrofitBuilder() {
        return RetrofitClient.getInstance().getBuilder();
    }

    public Retrofit getRetrofit() {
        return RetrofitClient.getInstance().getRetrofit();
    }
}
