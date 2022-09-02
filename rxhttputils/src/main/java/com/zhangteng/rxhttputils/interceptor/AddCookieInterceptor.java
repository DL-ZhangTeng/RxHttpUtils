package com.zhangteng.rxhttputils.interceptor;

import android.util.Log;

import com.zhangteng.rxhttputils.config.SPConfig;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.utils.SPUtilsKt;

import java.io.IOException;
import java.util.HashSet;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by swing on 2018/4/24.
 */
public class AddCookieInterceptor implements Interceptor, PriorityInterceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder builder = chain.request().newBuilder();
        HashSet<String> preferences = (HashSet<String>) SPUtilsKt.getFromSPToSet(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, SPConfig.COOKIE, new HashSet<String>());
        if (preferences != null) {
            for (String cookie : preferences) {
                builder.addHeader("Cookie", cookie);
                Log.v("RxHttpUtils", "Adding Header Cookie : " + cookie);
            }
        }
        return chain.proceed(builder.build());
    }

    @Override
    public int getPriority() {
        return 2;
    }
}
