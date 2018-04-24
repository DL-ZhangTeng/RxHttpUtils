package com.zhangteng.rxhttputils.interceptor;


import android.app.Dialog;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

public class ObservableTransformer<T> implements io.reactivex.ObservableTransformer<T, T> {

    private Dialog mProgressDialog;

    public ObservableTransformer() {
    }

    public ObservableTransformer(Dialog mProgressDialog) {
        this.mProgressDialog = mProgressDialog;
    }

    @Override
    public ObservableSource<T> apply(Observable<T> upstream) {
        return upstream
                .subscribeOn(Schedulers.io())
                .unsubscribeOn(Schedulers.io())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(@NonNull Disposable disposable) throws Exception {
                        if (mProgressDialog != null) {
                            mProgressDialog.show();
                        }
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread());
    }
}
