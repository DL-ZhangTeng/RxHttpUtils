package com.zhangteng.rxhttputils.observer.interfaces;

import io.reactivex.disposables.Disposable;

/**
 * Created by swing on 2018/4/24.
 */
public interface ISubscriber<T> {

    void doOnSubscribe(Disposable d);

    void doOnError(Throwable e);

    void doOnNext(T t);

    void doOnCompleted();
}
