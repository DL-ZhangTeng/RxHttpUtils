package com.zhangteng.rxhttputils.interceptor;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by swing on 2018/4/24.
 */
public class HeaderInterceptor implements Interceptor, PriorityInterceptor {
    private Map<String, Object> headerMaps;
    private Function<Map<String, Object>, Map<String, Object>> headersFunction;

    /**
     * description 设置请求头公共参数
     */
    public HeaderInterceptor() {

    }

    /**
     * description 设置请求头公共参数
     *
     * @param headerMaps 请求头设置的静态参数
     */
    public HeaderInterceptor(Map<String, Object> headerMaps) {
        this.headerMaps = headerMaps;
    }

    /**
     * description 动态设置请求头，如token等需要根据登录状态实时变化的请求头参数，最小支持api 24
     *
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public HeaderInterceptor(Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        this.headersFunction = headersFunction;
    }

    /**
     * description 设置请求头公共参数，最小支持api 24
     *
     * @param headerMaps      请求头设置的静态参数
     * @param headersFunction 请求头设置的函数式参数，如token等需要根据登录状态实时变化的请求头参数
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    public HeaderInterceptor(Map<String, Object> headerMaps, Function<Map<String, Object>, Map<String, Object>> headersFunction) {
        this.headerMaps = headerMaps;
        this.headersFunction = headersFunction;
    }

    @NonNull
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request.Builder request = chain.request().newBuilder();
        if (headersFunction != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (headerMaps == null) {
                headerMaps = new HashMap<>();
            }
            headerMaps = headersFunction.apply(headerMaps);
        }

        if (headerMaps != null && headerMaps.size() > 0) {
            for (Map.Entry<String, Object> entry : headerMaps.entrySet()) {
                request.addHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        return chain.proceed(request.build());
    }

    @Override
    public int getPriority() {
        return 1;
    }

    public void setHeaderMaps(Map<String, Object> headerMaps) {
        this.headerMaps = headerMaps;
    }

    public Map<String, Object> getHeaderMaps() {
        return headerMaps;
    }

}
