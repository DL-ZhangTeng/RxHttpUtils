package com.zhangteng.rxhttputils.fileload.download;

import io.reactivex.Observer;
import io.reactivex.annotations.NonNull;
import okhttp3.ResponseBody;

/**
 * Created by swing on 2018/4/24.
 */

public abstract class BaseDownloadObserver implements Observer<ResponseBody> {

    /**
     * 失败回调
     *
     * @param e 错误信息
     */
    protected abstract void doOnError(Throwable e);

    @Override
    public void onError(@NonNull Throwable e) {
        doOnError(e);
    }
}
