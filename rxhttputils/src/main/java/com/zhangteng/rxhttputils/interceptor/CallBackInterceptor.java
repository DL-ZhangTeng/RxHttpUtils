package com.zhangteng.rxhttputils.interceptor;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * description: 网络请求前后回调函数
 * author: Swing
 * date: 2022/9/1
 */
public class CallBackInterceptor implements Interceptor, PriorityInterceptor {
    private final HttpHandler httpHandler;

    public CallBackInterceptor(HttpHandler httpHandler) {
        this.httpHandler = httpHandler;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        if (httpHandler != null) {
            //在请求服务器之前拿到
            Request request = httpHandler.onHttpRequestBefore(chain, chain.request());
            Response response = chain.proceed(request);
            //这里可以比客户端提前一步拿到服务器返回的结果
            return httpHandler.onHttpResultResponse(chain, response);
        } else {
            return chain.proceed(chain.request());
        }
    }

    /**
     * description 晚于{@link CacheInterceptor} {@link HeaderInterceptor} {@link AddCookieInterceptor}执行
     *             早于{@link SignInterceptor} {@link EncryptionInterceptor}执行
     */
    @Override
    public int getPriority() {
        return 3;
    }

    /**
     * 处理 Http 请求和响应结果的处理类
     */
    public interface HttpHandler {

        /**
         * 这里可以先客户端一步拿到每一次 Http 请求的结果
         *
         * @param chain    {@link okhttp3.Interceptor.Chain}
         * @param response {@link Response}
         * @return {@link Response}
         */
        @NonNull
        Response onHttpResultResponse(@NonNull Interceptor.Chain chain, @NonNull Response response);

        /**
         * 这里可以在请求服务器之前拿到 {@link Request}
         *
         * @param chain   {@link okhttp3.Interceptor.Chain}
         * @param request {@link Request}
         * @return {@link Request}
         */
        @NonNull
        Request onHttpRequestBefore(@NonNull Interceptor.Chain chain, @NonNull Request request);

        /**
         * 空实现
         */
        HttpHandler EMPTY = new HttpHandler() {

            @NonNull
            @Override
            public Response onHttpResultResponse(@NonNull Interceptor.Chain chain, @NonNull Response response) {
                //都必须将 response 返回出去
                return response;
            }

            @NonNull
            @Override
            public Request onHttpRequestBefore(@NonNull Interceptor.Chain chain, @NonNull Request request) {
                //都必须将 request 返回出去
                return request;
            }
        };
    }
}
