package com.zhangteng.rxhttputils.observer.interfaces;

import com.zhangteng.utils.IException;

import io.reactivex.disposables.Disposable;

/**
 * Created by swing on 2018/4/24.
 */
public interface ISubscriber<T> {

    void doOnSubscribe(Disposable d);

    void doOnError(IException iException);

    void doOnNext(T t);

    void doOnCompleted();
}
