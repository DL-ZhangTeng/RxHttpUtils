package com.zhangteng.rxhttputils.transformer;


import android.app.Dialog;

import org.jetbrains.annotations.NotNull;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

/**
 * description: 带加载中动画处理的数据源转换器
 * author: Swing
 * date: 2021/10/9
 */
public class ProgressDialogObservableTransformer<T> implements ObservableTransformer<T, T> {

    private Dialog mProgressDialog;

    public ProgressDialogObservableTransformer() {
    }

    public ProgressDialogObservableTransformer(Dialog mProgressDialog) {
        this.mProgressDialog = mProgressDialog;
    }

    @NotNull
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
