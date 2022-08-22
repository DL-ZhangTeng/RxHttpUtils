package com.zhangteng.app;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.observer.CommonObserver;
import com.zhangteng.utils.IException;
import com.zhangteng.utils.LogUtilsKt;
import com.zhangteng.utils.ToastUtilsKt;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Dialog mProgressDialog = new Dialog(this, R.style.progress_dialog);
        View view = View.inflate(this, R.layout.layout_dialog_progress, null);
        ImageView mImageView = view.findViewById(R.id.progress_bar);
        ((AnimationDrawable) mImageView.getDrawable()).start();
        mProgressDialog.setContentView(view);

//        HttpUtils.getInstance()
//                .ConfigGlobalHttpUtils()
//                .createService(ApiService.class)
//                .loginPwd("admin", "admin")
//                .compose(new ProgressDialogObservableTransformer<>(mProgressDialog))
//                .subscribe(new CommonObserver<BaseResponse<LoginBean>>() {
//                    @Override
//                    protected void onFailure(IException iException) {
//
//                    }
//
//                    @Override
//                    protected void onSuccess(BaseResponse<LoginBean> bean) {
//                        Toast.makeText(MainActivity.this, bean.getMsg(), Toast.LENGTH_LONG);
//                    }
//                });
//        HttpUtils.getInstance()
//                .ConfigSingleInstance()
//                .setBaseUrl("https://**/")
//                .createService(ApiService.class)
//                .loginPwd("admin", "admin")
//                .compose(new LifecycleObservableTransformer<>(MainActivity.this))
//                .compose(new ProgressDialogObservableTransformer<>(mProgressDialog))
//                .subscribe(new BaseObserver<BaseResponse<LoginBean>>() {
//                    @Override
//                    public void doOnSubscribe(Disposable d) {
//
//                    }
//
//                    @Override
//                    public void doOnError(IException iException) {
//
//                    }
//
//                    @Override
//                    public void doOnNext(BaseResponse<LoginBean> loginBeanBaseResponse) {
//                        ToastUtilsKt.showShortToast(MainActivity.this, loginBeanBaseResponse.getMsg());
//                    }
//
//                    @Override
//                    public void doOnCompleted() {
//
//                    }
//                });

        HttpUtils.getInstance()
                .ConfigSingleInstance()
                .setBaseUrl("https://**/")
                .createService(ApiService.class)
                .loginPwd("admin", "admin")
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(disposable -> mProgressDialog.show())
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CommonObserver<BaseResponse<LoginBean>>(mProgressDialog) {
                    @Override
                    protected void onFailure(IException iException) {
                        LogUtilsKt.e(iException.getMessage());
                    }

                    @Override
                    protected void onSuccess(BaseResponse<LoginBean> loginBeanBaseResponse) {
                        ToastUtilsKt.showShortToast(MainActivity.this, loginBeanBaseResponse.getMsg());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        HttpUtils.getInstance().cancelSingleRequest(this);
//        HttpUtils.getInstance().cancelSingleRequest(Disposable);
//        HttpUtils.getInstance().cancelAllRequest();
    }
}