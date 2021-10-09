package com.zhangteng.rxhttputils.transformer;


import androidx.lifecycle.LifecycleOwner;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.lifecycle.HttpLifecycleEventObserver;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * description: 带生命周期处理的数据源转换器
 * author: Swing
 * date: 2021/10/9
 */
public class LifecycleObservableTransformer<T> implements ObservableTransformer<T, T> {

    private Object tag;
    private Disposable disposable;

    public LifecycleObservableTransformer() {
    }

    public LifecycleObservableTransformer(Object tag) {
        this.tag = tag;
        if (tag instanceof LifecycleOwner) {
            HttpLifecycleEventObserver.bind((LifecycleOwner) tag);
        }
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> {
                    this.disposable = d;
                    if (tag == null) {
                        HttpUtils.getInstance().addDisposable(d);
                    } else {
                        HttpUtils.getInstance().addDisposable(d, tag);
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    if (disposable != null) {
                        HttpUtils.getInstance().cancelSingleRequest(disposable);
                        disposable = null;
                    }
                });
    }
}
