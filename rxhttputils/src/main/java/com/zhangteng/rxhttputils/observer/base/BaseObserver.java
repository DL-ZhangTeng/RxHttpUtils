package com.zhangteng.rxhttputils.observer.base;


import com.zhangteng.rxhttputils.observer.interfaces.ISubscriber;
import com.zhangteng.utils.IException;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by swing on 2018/4/24.
 */
public abstract class BaseObserver<T> implements Observer<T>, ISubscriber<T> {

    protected boolean isHideToast() {
        return false;
    }

    @Override
    public void onSubscribe(Disposable d) {
        doOnSubscribe(d);
    }

    @Override
    public void onNext(T o) {
        doOnNext(o);
    }

    @Override
    public void onError(Throwable e) {
        doOnError(IException.Companion.handleException(e));
    }

    @Override
    public void onComplete() {
        doOnCompleted();
    }
}
