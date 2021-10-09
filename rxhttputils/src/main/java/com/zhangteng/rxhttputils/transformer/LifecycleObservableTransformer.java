package com.zhangteng.rxhttputils.transformer;


import androidx.lifecycle.LifecycleOwner;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.lifecycle.HttpLifecycleEventObserver;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.schedulers.Schedulers;

/**
 * description: 带生命周期处理的数据源转换器
 * author: Swing
 * date: 2021/10/9
 */
public class LifecycleObservableTransformer<T> implements ObservableTransformer<T, T> {

    private final Object tag;

    public LifecycleObservableTransformer(Object tag) {
        this.tag = tag;
        if (tag instanceof LifecycleOwner) {
            HttpLifecycleEventObserver.bind((LifecycleOwner) tag);
        }
    }

    @NotNull
    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> HttpUtils.getInstance().addDisposable(disposable, tag));
    }
}
