package com.zhangteng.rxhttputils.lifecycle;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.zhangteng.rxhttputils.http.HttpUtils;

/**
 * @description: Http请求取消生命周期观察者
 * @author: Swing
 * @date: 2021/9/22
 */
public final class HttpLifecycleEventObserver implements LifecycleEventObserver {

    /**
     * 绑定组件的生命周期
     *
     * @param lifecycleOwner 请传入 AppCompatActivity 或者 AndroidX.Fragment 子类
     *                       如需传入其他对象可继承{@link androidx.lifecycle.LifecycleOwner }
     *                       请参考以下两个类{@link androidx.lifecycle.LifecycleService }{@link androidx.lifecycle.ProcessLifecycleOwner}
     */
    public static void bind(LifecycleOwner lifecycleOwner) {
        lifecycleOwner.getLifecycle().addObserver(new HttpLifecycleEventObserver());
    }

    /**
     * 判断宿主是否处于活动状态
     */
    public static boolean isLifecycleActive(LifecycleOwner lifecycleOwner) {
        return lifecycleOwner != null && lifecycleOwner.getLifecycle().getCurrentState() != Lifecycle.State.DESTROYED;
    }

    @Override
    public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
        if (event != Lifecycle.Event.ON_DESTROY) {
            return;
        }

        // 移除监听
        source.getLifecycle().removeObserver(this);
        // 取消请求
        HttpUtils.getInstance().cancelSingleRequest(source);
    }
}