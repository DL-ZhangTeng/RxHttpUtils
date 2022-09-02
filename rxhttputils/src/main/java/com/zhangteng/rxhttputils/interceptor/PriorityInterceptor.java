package com.zhangteng.rxhttputils.interceptor;

/**
 * description: 拦截器优先级接口，优先级越小越早被添加0-9预留给框架
 * author: Swing
 * date: 2022/9/2
 */
public interface PriorityInterceptor {
    default int getPriority() {
        return 10;
    }
}
