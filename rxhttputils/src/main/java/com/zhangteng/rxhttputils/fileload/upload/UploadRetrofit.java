package com.zhangteng.rxhttputils.fileload.upload;


import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.transformer.ProgressDialogObservableTransformer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by swing on 2018/4/24.
 */

public class UploadRetrofit {

    private static UploadRetrofit instance;
    private Retrofit mRetrofit;

    private UploadRetrofit() {
        mRetrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .baseUrl(HttpUtils.getInstance().ConfigGlobalHttpUtils().getRetrofit().baseUrl())
                .build();
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
        return mRetrofit;
    }

    /**
     * description 上传文件
     *
     * @param uploadUrl 后台url
     * @param filePath  文件路径
     * @return Observable<ResponseBody>
     */
    public static Observable<ResponseBody> uploadFile(String uploadUrl, String filePath) {
        return uploadFile(uploadUrl, "uploaded_file", filePath);
    }

    /**
     * description 上传文件
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
                .uploadImg(uploadUrl, body);
    }

    /**
     * description 上传文件
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
     * description 上传文件
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
                .uploadImgs(uploadUrl, parts)
                .compose(new ProgressDialogObservableTransformer<>());
    }
}
