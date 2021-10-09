package com.zhangteng.rxhttputils.observer;

import android.app.Dialog;

import androidx.lifecycle.LifecycleOwner;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.lifecycle.HttpLifecycleEventObserver;
import com.zhangteng.rxhttputils.observer.base.BaseObserver;
import com.zhangteng.rxhttputils.utils.ToastUtils;

import io.reactivex.disposables.Disposable;

/**
 * Created by swing on 2018/4/24.
 */
public abstract class CommonObserver<T> extends BaseObserver<T> {


    private Dialog mProgressDialog;

    private Disposable disposable;
    private Object tag;

    public CommonObserver() {
    }


    public CommonObserver(Object tag) {
        this.tag = tag;
        if (tag instanceof LifecycleOwner) {
            HttpLifecycleEventObserver.bind((LifecycleOwner) tag);
        }
    }

    public CommonObserver(Dialog progressDialog) {
        mProgressDialog = progressDialog;
    }

    public CommonObserver(Dialog mProgressDialog, Object tag) {
        this.mProgressDialog = mProgressDialog;
        this.tag = tag;
        if (tag instanceof LifecycleOwner) {
            HttpLifecycleEventObserver.bind((LifecycleOwner) tag);
        }
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
        this.disposable = d;
        if (tag == null) {
            HttpUtils.getInstance().addDisposable(d);
        } else {
            HttpUtils.getInstance().addDisposable(d, tag);
        }
    }

    @Override
    public void doOnError(String errorMsg) {
        if (disposable != null) {
            HttpUtils.getInstance().cancelSingleRequest(disposable);
            disposable = null;
        }
        if (isTargetDestroy()) return;
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
        if (disposable != null) {
            HttpUtils.getInstance().cancelSingleRequest(disposable);
            disposable = null;
        }
        if (isTargetDestroy()) return;
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
        }
    }

    @Override
    public void doOnNext(T t) {
        if (isTargetDestroy()) return;
        onSuccess(t);
    }

    /**
     * @description 目标是否销毁
     */
    private boolean isTargetDestroy() {
        return tag != null
                && tag instanceof LifecycleOwner
                && !HttpLifecycleEventObserver.isLifecycleActive((LifecycleOwner) tag);
    }
}
