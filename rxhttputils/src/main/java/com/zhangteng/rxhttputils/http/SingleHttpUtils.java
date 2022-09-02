package com.zhangteng.rxhttputils.http;

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
import com.zhangteng.utils.SSLUtils;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
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
    private Dns dns;

    private Cache cache;

    private long readTimeout;
    private long writeTimeout;
    private long connectTimeout;

    private SSLUtils.SSLParams sslParams;

    private final List<Converter.Factory> converterFactories = new ArrayList<>();
    private final List<CallAdapter.Factory> adapterFactories = new ArrayList<>();

    /**
     * description: 拦截器集合,按照优先级从小到大排序
     */
    private final TreeSet<PriorityInterceptor> priorityInterceptors;
    /**
     * description: 网络拦截器集合,按照优先级从小到大排序
     */
    private final TreeSet<PriorityInterceptor> networkInterceptors;

    private SingleHttpUtils() {
        priorityInterceptors = new TreeSet<>((o, r) -> Integer.compare(o.getPriority(), r.getPriority()));
        networkInterceptors = new TreeSet<>((o, r) -> Integer.compare(o.getPriority(), r.getPriority()));
    }

    public static SingleHttpUtils getInstance() {
        if (instance == null) {
            synchronized (SingleHttpUtils.class) {
                if (instance == null) {
                    instance = new SingleHttpUtils();
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
     * 局部设置CallAdapter.Factory,默认RxJava2CallAdapterFactory.create()
     */
    public SingleHttpUtils addCallAdapterFactory(CallAdapter.Factory factory) {
        if (factory != null) {
            adapterFactories.add(factory);
        }
        return this;
    }

    /**
     * description 设置域名解析服务器
     *
     * @param dns 域名解析服务器
     */
    public SingleHttpUtils setDns(Dns dns) {
        this.dns = dns;
        return this;
    }

    /**
     * description 添加单个请求头公共参数，当okHttpClient构建完成后依旧可以新增全局请求头参数，可随时添加修改公共请求头
     *
     * @param key   请求头 key
     * @param value 请求头 value
     */
    public SingleHttpUtils addHeader(String key, Object value) {
        Interceptor headerInterceptor = null;
        for (Interceptor interceptor : priorityInterceptors) {
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
    public SingleHttpUtils setHeaders(Map<String, Object> headerMaps) {
        priorityInterceptors.add(new HeaderInterceptor(headerMaps));
        return this;
    }

    /**
     * description 动态设置请求头，如token等需要根据登录状态实时变化的请求头参数，最小支持api 24
     *
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public SingleHttpUtils setHeaders(Function<Map<String, Object>, Map<String, Object>> headersFunction) {
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
    public SingleHttpUtils setHeaders(Map<String, Object> headerMaps, Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        priorityInterceptors.add(new HeaderInterceptor(headerMaps, headersFunction));
        return this;
    }

    /**
     * description 开启网络日志
     *
     * @param isShowLog 是否
     */
    public SingleHttpUtils setLog(boolean isShowLog) {
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
    public SingleHttpUtils setLog(HttpLoggingInterceptor.Logger logger) {
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
    public SingleHttpUtils setCache(boolean isCache) {
        if (isCache) {
            CacheInterceptor cacheInterceptor = new CacheInterceptor();
            File file = new File(FileUtilsKt.getDiskCacheDir(HttpUtils.getInstance().getContext()) + "/RxHttpUtilsCache");
            cache = new Cache(file, 1024 * 1024);
            priorityInterceptors.add(cacheInterceptor);
            networkInterceptors.add(cacheInterceptor);
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
    public SingleHttpUtils setCache(boolean isCache, String path, long maxSize) {
        if (isCache) {
            CacheInterceptor cacheInterceptor = new CacheInterceptor();
            File file = new File(path);
            cache = new Cache(file, maxSize);
            priorityInterceptors.add(cacheInterceptor);
            networkInterceptors.add(cacheInterceptor);
        }
        return this;
    }

    /**
     * description 设置Cookie
     *
     * @param saveCookie 是否设置Cookie
     */
    public SingleHttpUtils setCookie(boolean saveCookie) {
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
    public SingleHttpUtils setHttpCallBack(CallBackInterceptor.CallBack callBack) {
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
    public SingleHttpUtils setSign(String appKey) {
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
    public SingleHttpUtils setEnAndDecryption(HttpUrl publicKeyUrl, String publicKey) {
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
    public SingleHttpUtils addInterceptor(PriorityInterceptor interceptor) {
        priorityInterceptors.add(interceptor);
        return this;
    }

    /**
     * description 添加拦截器
     *
     * @param interceptors 带优先级的拦截器
     */
    public SingleHttpUtils addInterceptors(List<PriorityInterceptor> interceptors) {
        priorityInterceptors.addAll(interceptors);
        return this;
    }

    /**
     * description 添加网络拦截器
     *
     * @param interceptor 带优先级的拦截器
     */
    public SingleHttpUtils addNetworkInterceptor(PriorityInterceptor interceptor) {
        networkInterceptors.add(interceptor);
        return this;
    }

    /**
     * description 添加网络拦截器
     *
     * @param interceptors 带优先级的拦截器
     */
    public SingleHttpUtils addNetworkInterceptors(List<PriorityInterceptor> interceptors) {
        networkInterceptors.addAll(interceptors);
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
            List<Converter.Factory> listConverterFactory = GlobalHttpUtils.getInstance().getRetrofit().converterFactories();
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
            List<CallAdapter.Factory> listAdapterFactory = GlobalHttpUtils.getInstance().getRetrofit().callAdapterFactories();
            for (CallAdapter.Factory factory : listAdapterFactory) {
                singleRetrofitBuilder.addCallAdapterFactory(factory);
            }

        } else {
            for (CallAdapter.Factory adapterFactory : adapterFactories) {
                singleRetrofitBuilder.addCallAdapterFactory(adapterFactory);
            }
        }


        if (baseUrl == null || baseUrl.isEmpty()) {
            singleRetrofitBuilder.baseUrl(GlobalHttpUtils.getInstance().getRetrofit().baseUrl());
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

        if (dns != null) {
            singleOkHttpBuilder.dns(dns);
        }

        if (cache != null) {
            singleOkHttpBuilder.cache(cache);
        }
        for (PriorityInterceptor priorityInterceptor : priorityInterceptors) {
            singleOkHttpBuilder.addInterceptor(priorityInterceptor);
        }
        for (PriorityInterceptor priorityInterceptor : networkInterceptors) {
            singleOkHttpBuilder.addNetworkInterceptor(priorityInterceptor);
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
