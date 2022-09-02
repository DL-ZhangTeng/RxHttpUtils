package com.zhangteng.rxhttputils.interceptor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.NonNull;

import com.zhangteng.rxhttputils.http.HttpUtils;

import java.io.IOException;

import okhttp3.CacheControl;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 缓存数据，如果无网络且有缓存有数据直接读取缓存数据，只针对get请求
 * Created by swing on 2018/4/24.
 */
public class CacheInterceptor implements PriorityInterceptor {
    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        //如果没有网络，则启用 FORCE_CACHE
        if (!isNetworkConnected()) {
            request = request.newBuilder()
                    .cacheControl(CacheControl.FORCE_CACHE)
                    .build();
        }
        Response originalResponse = chain.proceed(request);
        if (isNetworkConnected()) {
            //有网的时候读接口上的@Headers里的配置
            String cacheControl = request.cacheControl().toString();
            return originalResponse.newBuilder()
                    .header("Cache-Control", cacheControl)
                    .removeHeader("Pragma")
                    .build();
        } else {
            return originalResponse.newBuilder()
                    .header("Cache-Control", "public, only-if-cached, max-stale=3600")
                    .removeHeader("Pragma")
                    .build();
        }
    }

    @Override
    public int getPriority() {
        return 0;
    }

    public static boolean isNetworkConnected() {
        Context context = HttpUtils.getInstance().getContext();
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            @SuppressLint("MissingPermission") NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return true;
    }
}
