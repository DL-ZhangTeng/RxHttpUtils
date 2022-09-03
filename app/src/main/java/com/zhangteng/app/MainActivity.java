package com.zhangteng.app;

import android.app.Dialog;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.interceptor.CallBackInterceptor;
import com.zhangteng.rxhttputils.observer.CommonObserver;
import com.zhangteng.rxhttputils.transformer.ProgressDialogObservableTransformer;
import com.zhangteng.utils.IException;
import com.zhangteng.utils.LogUtilsKt;
import com.zhangteng.utils.ToastUtilsKt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

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

        HttpUtils.getInstance()
                .ConfigGlobalHttpUtils()
                .createService(ApiService.class)
                .loginPwd("admin", "admin")
                .compose(new ProgressDialogObservableTransformer<>(mProgressDialog))
                .subscribe(new CommonObserver<BaseResponse<LoginBean>>() {
                    @Override
                    protected void onFailure(IException iException) {
                        LogUtilsKt.e(iException.getMessage());
                    }

                    @Override
                    protected void onSuccess(BaseResponse<LoginBean> bean) {
                        Toast.makeText(MainActivity.this, bean.getMsg(), Toast.LENGTH_LONG);
                    }
                });
//        HttpUtils.getInstance()
//                .ConfigSingleInstance()
//                .setBaseUrl("https://www.baidu.com")
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

//        HttpUtils.getInstance()
//                .ConfigSingleInstance()
//                .setBaseUrl("https://www.baidu.com")
//                .setHttpCallBack(new CallBackInterceptor.CallBack() {
//                    @NonNull
//                    @Override
//                    public Response onHttpResponse(@NonNull Interceptor.Chain chain, @NonNull Response response) {
//                        //这里可以先客户端一步拿到每一次 Http 请求的结果
//                        ResponseBody body = response.newBuilder().build().body();
//                        BufferedSource source = body.source();
//                        try {
//                            source.request(Long.MAX_VALUE); // Buffer the entire body.
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//                        Buffer buffer = source.getBuffer();
//                        Charset charset = StandardCharsets.UTF_8;
//                        MediaType contentType = body.contentType();
//                        if (contentType != null) {
//                            charset = contentType.charset(charset);
//                        }
//                        LogUtilsKt.e(buffer.readString(charset));
//                        return response;
//                    }
//
//                    @NonNull
//                    @Override
//                    public Request onHttpRequest(@NonNull Interceptor.Chain chain, @NonNull Request request) {
//                        //这里可以在请求服务器之前拿到
//                        LogUtilsKt.e(new Gson().toJson(request.headers()));
//                        RequestBody body = request.newBuilder().build().body();
//                        if (body != null) {
//                            try {
//                                LogUtilsKt.e(String.valueOf(body.contentLength()));
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }
//                        return request;
//                    }
//                })
//                .createService(ApiService.class)
//                .loginPwd("admin", "admin")
//                .subscribeOn(Schedulers.io())
//                .doOnSubscribe(disposable -> mProgressDialog.show())
//                .subscribeOn(AndroidSchedulers.mainThread())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new CommonObserver<BaseResponse<LoginBean>>(mProgressDialog) {
//                    @Override
//                    protected void onFailure(IException iException) {
//                        LogUtilsKt.e(iException.getMessage());
//                    }
//
//                    @Override
//                    protected void onSuccess(BaseResponse<LoginBean> loginBeanBaseResponse) {
//                        ToastUtilsKt.showShortToast(MainActivity.this, loginBeanBaseResponse.getMsg());
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