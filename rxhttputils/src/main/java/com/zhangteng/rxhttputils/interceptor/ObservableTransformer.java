package com.zhangteng.rxhttputils.interceptor;


import android.app.Dialog;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
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
                .doOnSubscribe(disposable -> {
                    if (mProgressDialog != null) {
                        mProgressDialog.show();
                    }
                })
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
                        mProgressDialog.dismiss();
                    }
                });
    }
}
