package com.zhangteng.rxhttputils.observer;

import android.app.Dialog;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.observer.base.BaseObserver;
import com.zhangteng.rxhttputils.utils.ToastUtils;

import io.reactivex.disposables.Disposable;

/**
 * Created by swing on 2018/4/24.
 */
public abstract class CommonObserver<T> extends BaseObserver<T> {


    private Dialog mProgressDialog;

    public CommonObserver() {
    }

    public CommonObserver(Dialog progressDialog) {
        mProgressDialog = progressDialog;
    }

    /**
     * 失败回调
     *
     * @param errorMsg
     */
    protected abstract void onFailure(String errorMsg);

    /**
     * 成功回调
     *
     * @param t
     */
    protected abstract void onSuccess(T t);


    @Override
    public void doOnSubscribe(Disposable d) {
        HttpUtils.getInstance().addDisposable(d);
    }

    @Override
    public void doOnError(String errorMsg) {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
        if (!isHideToast()) {
            ToastUtils.showShort(HttpUtils.getInstance().getContext(), errorMsg);
        }
        onFailure(errorMsg);
    }


    @Override
    public void doOnCompleted() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void doOnNext(T t) {
        onSuccess(t);
    }
}
