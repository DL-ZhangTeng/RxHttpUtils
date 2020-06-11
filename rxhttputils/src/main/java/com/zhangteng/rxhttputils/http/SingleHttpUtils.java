package com.zhangteng.rxhttputils.http;

import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.zhangteng.rxhttputils.interceptor.AddCookieInterceptor;
import com.zhangteng.rxhttputils.interceptor.CacheInterceptor;
import com.zhangteng.rxhttputils.interceptor.HeaderInterceptor;
import com.zhangteng.rxhttputils.interceptor.SaveCookieInterceptor;
import com.zhangteng.rxhttputils.utils.SSLUtils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
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
    private boolean cache = false;
    private boolean saveCookie = true;

    private String cachePath;
    private long cacheMaxSize;

    private long readTimeout;
    private long writeTimeout;
    private long connectTimeout;

    private SSLUtils.SSLParams sslParams;

    private List<Converter.Factory> converterFactories = new ArrayList<>();
    private List<CallAdapter.Factory> adapterFactories = new ArrayList<>();

    public static SingleHttpUtils getInstance() {
        instance = new SingleHttpUtils();
        return instance;
    }

    public SingleHttpUtils baseUrl(String baseUrl) {
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

    public SingleHttpUtils addHeaders(Map<String, Object> headerMaps) {
        this.headerMaps = headerMaps;
        return this;
    }

    public SingleHttpUtils log(boolean isShowLog) {
        this.isShowLog = isShowLog;
        return this;
    }

    public SingleHttpUtils cache(boolean cache) {
        this.cache = cache;
        return this;
    }

    public SingleHttpUtils saveCookie(boolean saveCookie) {
        this.saveCookie = saveCookie;
        return this;
    }

    public SingleHttpUtils cachePath(String cachePath, long maxSize) {
        this.cachePath = cachePath;
        this.cacheMaxSize = maxSize;
        return this;
    }

    public SingleHttpUtils readTimeout(long readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public SingleHttpUtils writeTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
        return this;
    }

    public SingleHttpUtils connectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * 信任所有证书,不安全有风险
     *
     * @return
     */
    public SingleHttpUtils sslSocketFactory() {
        sslParams = SSLUtils.getSslSocketFactory();
        return this;
    }

    /**
     * 使用预埋证书，校验服务端证书（自签名证书）
     *
     * @param certificates
     * @return
     */
    public SingleHttpUtils sslSocketFactory(InputStream... certificates) {
        sslParams = SSLUtils.getSslSocketFactory(certificates);
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
    public SingleHttpUtils sslSocketFactory(InputStream bksFile, String password, InputStream... certificates) {
        sslParams = SSLUtils.getSslSocketFactory(bksFile, password, certificates);
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
        return getSingleRetrofitBuilder().build().create(cls);
    }


    /**
     * 单个RetrofitBuilder
     *
     * @return
     */
    public Retrofit.Builder getSingleRetrofitBuilder() {

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


        if (TextUtils.isEmpty(baseUrl)) {
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

        if (cache) {
            CacheInterceptor cacheInterceptor = new CacheInterceptor();
            Cache cache;
            if (!TextUtils.isEmpty(cachePath) && cacheMaxSize > 0) {
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
            HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    Log.e("SingleHttpUtils", message);
                }
            });
            loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            singleOkHttpBuilder.addInterceptor(loggingInterceptor);
        }

        if (saveCookie) {
            singleOkHttpBuilder
                    .addInterceptor(new AddCookieInterceptor())
                    .addInterceptor(new SaveCookieInterceptor());
        }

        singleOkHttpBuilder.readTimeout(readTimeout > 0 ? readTimeout : 10, TimeUnit.SECONDS);

        singleOkHttpBuilder.writeTimeout(writeTimeout > 0 ? writeTimeout : 10, TimeUnit.SECONDS);

        singleOkHttpBuilder.connectTimeout(connectTimeout > 0 ? connectTimeout : 10, TimeUnit.SECONDS);

        if (sslParams != null) {
            singleOkHttpBuilder.sslSocketFactory(sslParams.sSLSocketFactory, sslParams.trustManager);
        }

        return singleOkHttpBuilder;
    }
}
