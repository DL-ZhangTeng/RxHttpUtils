package com.zhangteng.rxhttputils.fileload.upload;


import android.text.TextUtils;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.transformer.ProgressDialogObservableTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.CallAdapter;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swing on 2018/4/24.
 */

public class UploadRetrofit {

    private static volatile UploadRetrofit instance;
    private Retrofit mRetrofit;
    private final Retrofit.Builder builder;

    private UploadRetrofit() {
        builder = new Retrofit.Builder()
                //默认使用全局配置
                .addCallAdapterFactory(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofitBuilder().callAdapterFactories().get(0))
                //默认使用全局配置
                .addConverterFactory(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofitBuilder().converterFactories().get(0))
                //默认使用全局baseUrl
                .baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofitBuilder().build().baseUrl())
                //默认使用全局配置
                .client(HttpUtils.getInstance().ConfigGlobalHttpUtils().getOkHttpClient());
    }

    public static UploadRetrofit getInstance() {
        if (instance == null) {
            synchronized (UploadRetrofit.class) {
                if (instance == null) {
                    instance = new UploadRetrofit();
                }
            }

        }
        return instance;
    }

    public Retrofit getRetrofit() {
        if (mRetrofit == null) {
            mRetrofit = builder.build();
        }
        return mRetrofit;
    }

    /**
     * description 自定义baseUrl
     *
     * @param baseUrl 公共url
     * @return UploadRetrofit
     */
    public UploadRetrofit setBaseUrl(String baseUrl) {
        if (TextUtils.isEmpty(baseUrl)) {
            builder.baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl());
        } else {
            builder.baseUrl(baseUrl);
        }
        return this;
    }

    /**
     * description 自定义baseUrl
     *
     * @param baseUrl 公共url
     * @return UploadRetrofit
     */
    public UploadRetrofit setBaseUrl(HttpUrl baseUrl) {
        if (baseUrl == null) {
            builder.baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl());
        } else {
            builder.baseUrl(baseUrl);
        }
        return this;
    }

    /**
     * description 设置Converter.Factory,传null时默认GsonConverterFactory.create()
     *
     * @param factory Converter.Factory
     * @return UploadRetrofit
     */
    public UploadRetrofit addConverterFactory(Converter.Factory factory) {
        if (factory != null) {
            builder.addConverterFactory(factory);
        } else {
            builder.addConverterFactory(GsonConverterFactory.create());
        }
        return this;
    }

    /**
     * description 设置CallAdapter.Factory,传null时默认RxJava2CallAdapterFactory.create()
     *
     * @param factory CallAdapter.Factory
     * @return UploadRetrofit
     */
    public UploadRetrofit addCallAdapterFactory(CallAdapter.Factory factory) {
        if (factory != null) {
            builder.addCallAdapterFactory(factory);
        } else {
            builder.addCallAdapterFactory(RxJava2CallAdapterFactory.create());
        }
        return this;
    }

    /**
     * description 自定义网络请求client
     *
     * @param client 网络请求client
     * @return UploadRetrofit
     */
    public UploadRetrofit setOkHttpClient(OkHttpClient client) {
        if (client == null) {
            builder.client(HttpUtils.getInstance().ConfigGlobalHttpUtils().getOkHttpClient());
        } else {
            builder.client(client);
        }
        return this;
    }

    /**
     * description 上传文件 默认使用全据配置，如需自定义可用UploadRetrofit初始化
     *
     * @param uploadUrl 后台url
     * @param filePath  文件路径
     * @return Observable<ResponseBody>
     */
    public static Observable<ResponseBody> uploadFile(String uploadUrl, String filePath) {
        return uploadFile(uploadUrl, "uploaded_file", filePath);
    }

    /**
     * description 上传文件 默认使用全据配置，如需自定义可用UploadRetrofit初始化
     *
     * @param uploadUrl 后台url
     * @param fieldName 后台接收图片流的参数名
     * @param filePath  文件路径
     * @return Observable<ResponseBody>
     */
    public static Observable<ResponseBody> uploadFile(String uploadUrl, String fieldName, String filePath) {
        File file = new File(filePath);

        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        MultipartBody.Part body =
                MultipartBody.Part.createFormData(fieldName, file.getName(), requestFile);

        return UploadRetrofit
                .getInstance()
                .getRetrofit()
                .create(UploadFileApi.class)
                .uploadFile(uploadUrl, body);
    }

    /**
     * description 上传文件 默认使用全据配置，如需自定义可用UploadRetrofit初始化
     *
     * @param uploadUrl 后台url
     * @param filePaths 文件路径
     * @return Observable<ResponseBody>
     */
    public static Observable<ResponseBody> uploadFiles(String uploadUrl, List<String> filePaths) {
        List<String> fieldNames = new ArrayList<>();
        for (int i = 0; i < filePaths.size(); i++) {
            fieldNames.add("uploaded_file" + i);
        }
        return uploadFiles(uploadUrl, fieldNames, filePaths);
    }

    /**
     * description 上传文件 默认使用全据配置，如需自定义可用UploadRetrofit初始化
     *
     * @param uploadUrl  后台url
     * @param fieldNames 后台接收图片流的参数名
     * @param filePaths  文件路径
     * @return Observable<ResponseBody>
     */
    public static Observable<ResponseBody> uploadFiles(String uploadUrl, List<String> fieldNames, List<String> filePaths) {

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        for (int i = 0; i < filePaths.size(); i++) {
            File file = new File(filePaths.get(i));
            RequestBody imageBody = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            //"uploaded_file"+i 后台接收图片流的参数名
            builder.addFormDataPart(fieldNames.get(i), file.getName(), imageBody);
        }

        List<MultipartBody.Part> parts = builder.build().parts();

        return UploadRetrofit
                .getInstance()
                .getRetrofit()
                .create(UploadFileApi.class)
                .uploadFiles(uploadUrl, parts)
                .compose(new ProgressDialogObservableTransformer<>());
    }
}
