package com.zhangteng.rxhttputils.http;

/**
 * Created by swing on 2018/4/24.
 */
public class OkHttpClient {
    private static OkHttpClient instance;
    private okhttp3.OkHttpClient.Builder builder;

    private OkHttpClient() {
        builder = new okhttp3.OkHttpClient.Builder();
    }

    public static OkHttpClient getInstance() {
        if (instance == null) {
            synchronized (OkHttpClient.class) {
                if (instance == null) {
                    instance = new OkHttpClient();
                }
            }
        }
        return instance;
    }

    public okhttp3.OkHttpClient.Builder getBuilder() {
        return builder;
    }

    public okhttp3.OkHttpClient getClient() {
        return builder.build();
    }
}
