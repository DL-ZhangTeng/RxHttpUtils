package com.zhangteng.rxhttputils.fileload.download;

import android.text.TextUtils;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.transformer.ProgressDialogObservableTransformer;

import io.reactivex.Observable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swing on 2018/4/24.
 */

public class DownloadRetrofit {

    private static volatile DownloadRetrofit instance;
    private Retrofit mRetrofit;
    private final Retrofit.Builder builder;

    private DownloadRetrofit() {
        builder = new Retrofit.Builder()
                //默认使用全局配置
                .addCallAdapterFactory(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofitBuilder().callAdapterFactories().get(0))
                //默认使用全局配置
                .addConverterFactory(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofitBuilder().converterFactories().get(0))
                //默认使用全局baseUrl
                .baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofitBuilder().build().baseUrl())
                //默认使用全局配置
                .client(HttpUtils.getInstance().ConfigGlobalHttpUtils().getOkHttpClient());
    }

    public static DownloadRetrofit getInstance() {
        if (instance == null) {
            synchronized (DownloadRetrofit.class) {
                if (instance == null) {
                    instance = new DownloadRetrofit();
                }
            }

        }
        return instance;
    }

    public Retrofit getRetrofit() {
        if (mRetrofit == null) {
            mRetrofit = builder.build();
        }
        return mRetrofit;
    }

    /**
     * description 自定义baseUrl
     *
     * @param baseUrl 公共url
     * @return DownloadRetrofit
     */
    public DownloadRetrofit setBaseUrl(String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) {
            builder.baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl());
        } else {
            builder.baseUrl(baseUrl);
        }
        return this;
    }

    /**
     * description 自定义baseUrl
     *
     * @param baseUrl 公共url
     * @return DownloadRetrofit
     */
    public DownloadRetrofit setBaseUrl(HttpUrl baseUrl) {
        if (baseUrl == null) {
            builder.baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl());
        } else {
            builder.baseUrl(baseUrl);
        }
        return this;
    }

    /**
     * description 设置Converter.Factory,传null时默认GsonConverterFactory.create()
     *
     * @param factory Converter.Factory
     * @return DownloadRetrofit
     */
    public DownloadRetrofit addConverterFactory(Converter.Factory factory) {
        if (factory != null) {
            builder.addConverterFactory(factory);
        } else {
            builder.addConverterFactory(GsonConverterFactory.create());
        }
        return this;
    }

    /**
     * description 设置CallAdapter.Factory,传null时默认RxJava2CallAdapterFactory.create()
     *
     * @param factory CallAdapter.Factory
     * @return DownloadRetrofit
     */
    public DownloadRetrofit addCallAdapterFactory(CallAdapter.Factory factory) {
        if (factory != null) {
            builder.addCallAdapterFactory(factory);
        } else {
            builder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
        }
        return this;
    }

    /**
     * description 自定义网络请求client
     *
     * @param client 网络请求client
     * @return DownloadRetrofit
     */
    public DownloadRetrofit setOkHttpClient(OkHttpClient client) {
        if (client == null) {
            builder.client(HttpUtils.getInstance().ConfigGlobalHttpUtils().getOkHttpClient());
        } else {
            builder.client(client);
        }
        return this;
    }

    /**
     * description 下载文件 默认使用全据配置，如需自定义可用DownloadRetrofit初始化
     *
     * @param fileUrl 文件网络路径
     * @return Observable<ResponseBody>
     */
    public static Observable<ResponseBody> downloadFile(String fileUrl) {
        return DownloadRetrofit
                .getInstance()
                .getRetrofit()
                .create(DownloadApi.class)
                .downloadFile(fileUrl)
                .compose(new ProgressDialogObservableTransformer<>());
    }
}
