package com.zhangteng.rxhttputils.interceptor;

import okhttp3.Interceptor;

/**
 * description: 拦截器优先级接口，优先级越小越早被添加0-9预留给框架
 * author: Swing
 * date: 2022/9/2
 */
public interface PriorityInterceptor extends Interceptor {
    /**
     * description 自定义拦截器Priority必须>=10
     * Interceptor添加顺序: {@link CacheInterceptor} {@link HeaderInterceptor} {@link AddCookieInterceptor} {@link CallBackInterceptor} {@link SignInterceptor} {@link HttpLoggingProxyInterceptor} {@link EncryptionInterceptor}
     * NetworkInterceptor添加顺序: {@link CacheInterceptor} {@link SaveCookieInterceptor} {@link DecryptionInterceptor}
     */
    default int getPriority() {
        return 10;
    }
}
