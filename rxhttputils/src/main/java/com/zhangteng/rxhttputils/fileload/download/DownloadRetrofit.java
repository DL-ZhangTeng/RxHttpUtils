package com.zhangteng.rxhttputils.fileload.download;

import android.text.TextUtils;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.transformer.ProgressDialogObservableTransformer;

import io.reactivex.Observable;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swing on 2018/4/24.
 */

public class DownloadRetrofit {

    private static DownloadRetrofit instance;
    private Retrofit mRetrofit;
    private final Retrofit.Builder builder;

    private DownloadRetrofit() {
        builder = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl())
                //默认使用全局baseUrl
                .baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl())
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
     * description 下载文件 默认使用全据配置，如需自定义可用UploadRetrofit初始化
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
                .compose(new ProgressDialogObservableTransformer<ResponseBody>());
    }
}
