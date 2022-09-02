package com.zhangteng.rxhttputils.interceptor;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpLoggingProxyInterceptor implements PriorityInterceptor {
    private final HttpLoggingInterceptor httpLoggingInterceptor;

    public HttpLoggingProxyInterceptor(HttpLoggingInterceptor httpLoggingInterceptor) {
        this.httpLoggingInterceptor = httpLoggingInterceptor;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        return httpLoggingInterceptor.intercept(chain);
    }


    @Override
    public int getPriority() {
        return 5;
    }
}
