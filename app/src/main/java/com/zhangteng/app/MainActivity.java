package com.zhangteng.app;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zhangteng.app.R;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.interceptor.ObservableTransformer;
import com.zhangteng.rxhttputils.observer.CommonObserver;
import com.zhangteng.rxhttputils.utils.ToastUtils;

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
                .compose(new ObservableTransformer<>(mProgressDialog))
                .subscribe(new CommonObserver<BaseResponse<LoginBean>>() {
                    @Override
                    protected void onFailure(String errorMsg) {

                    }

                    @Override
                    protected void onSuccess(BaseResponse<LoginBean> bean) {
                        ToastUtils.show(MainActivity.this, bean.getMsg(), Toast.LENGTH_LONG);
                    }
                });
    }
}