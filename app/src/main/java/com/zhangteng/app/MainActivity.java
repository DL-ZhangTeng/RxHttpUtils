package com.zhangteng.app;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.observer.CommonObserver;
import com.zhangteng.rxhttputils.observer.base.BaseObserver;
import com.zhangteng.rxhttputils.transformer.LifecycleObservableTransformer;
import com.zhangteng.rxhttputils.transformer.ProgressDialogObservableTransformer;
import com.zhangteng.rxhttputils.utils.ToastUtils;

import io.reactivex.disposables.Disposable;

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
//                .compose(new ObservableTransformer<>(mProgressDialog))
//                .subscribe(new CommonObserver<BaseResponse<LoginBean>>() {
//                    @Override
//                    protected void onFailure(String errorMsg) {
//
//                    }
//
//                    @Override
//                    protected void onSuccess(BaseResponse<LoginBean> bean) {
//                        ToastUtils.show(MainActivity.this, bean.getMsg(), Toast.LENGTH_LONG);
//                    }
//                });
        HttpUtils.getInstance()
                .ConfigSingleInstance()
                .setBaseUrl("http://**/")
                .createService(ApiService.class)
                .loginPwd("admin", "admin")
                .compose(new LifecycleObservableTransformer<>(MainActivity.this))
                .compose(new ProgressDialogObservableTransformer<>(mProgressDialog))
                .subscribe(new BaseObserver<BaseResponse<LoginBean>>() {
                    @Override
                    public void doOnSubscribe(Disposable d) {

                    }

                    @Override
                    public void doOnError(String errorMsg) {

                    }

                    @Override
                    public void doOnNext(BaseResponse<LoginBean> loginBeanBaseResponse) {
                        ToastUtils.show(MainActivity.this, loginBeanBaseResponse.getMsg(), Toast.LENGTH_LONG);
                    }

                    @Override
                    public void doOnCompleted() {

                    }
                });

//        HttpUtils.getInstance()
//                .ConfigSingleInstance()
//                .setBaseUrl("http://**/")
//                .createService(ApiService.class)
//                .loginPwd("admin", "admin")
//                .subscribe(new CommonObserver<BaseResponse<LoginBean>>() {
//                    @Override
//                    protected void onFailure(String errorMsg) {
//
//                    }
//
//                    @Override
//                    protected void onSuccess(BaseResponse<LoginBean> loginBeanBaseResponse) {
//                        ToastUtils.show(MainActivity.this, loginBeanBaseResponse.getMsg(), Toast.LENGTH_LONG);
//                    }
//                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        HttpUtils.getInstance().cancelSingleRequest(this);
//        HttpUtils.getInstance().cancelSingleRequest(Disposable);
//        HttpUtils.getInstance().cancelAllRequest();
    }
}