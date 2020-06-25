package com.zhangteng.rxhttputils.http;

import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swing on 2018/4/24.
 */
public class RetrofitClient {
    private static RetrofitClient instance;
    private Retrofit.Builder builder;
    private okhttp3.OkHttpClient okHttpClient;

    private RetrofitClient() {
        builder = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create());
    }

    public static RetrofitClient getInstance() {
        if (instance == null) {
            synchronized (RetrofitClient.class) {
                if (instance == null) {
                    instance = new RetrofitClient();
                }
            }
        }
        return instance;
    }

    public Retrofit.Builder getBuilder() {
        return builder;
    }

    public Retrofit getRetrofit() {
        if (okHttpClient == null) {
            okHttpClient = OkHttpClient.getInstance().getClient();
        }
        return builder.client(okHttpClient).build();
    }
}
