package com.zhangteng.rxhttputils.http;

import android.app.Application;
import android.content.Context;

import com.zhangteng.rxhttputils.config.SPConfig;
import com.zhangteng.rxhttputils.fileload.download.DownloadRetrofit;
import com.zhangteng.rxhttputils.fileload.upload.UploadRetrofit;
import com.zhangteng.rxhttputils.utils.SPUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import okhttp3.ResponseBody;

/**
 * Created by swing on 2018/4/24.
 */
public class HttpUtils {
    private static HttpUtils instance;
    private static Application context;
    private static List<Disposable> disposables;

    private HttpUtils() {
        disposables = new ArrayList<>();
    }

    public static HttpUtils getInstance() {
        if (instance == null) {
            synchronized (HttpUtils.class) {
                if (instance == null) {
                    instance = new HttpUtils();
                }
            }
        }
        return instance;
    }

    public static void init(Application app) {
        context = app;
    }

    public Context getContext() {
        checkInitialize();
        return context;
    }

    public GlobalHttpUtils ConfigGlobalHttpUtils() {
        return GlobalHttpUtils.getInstance();
    }

    public SingleHttpUtils ConfigSingleInstance() {
        return SingleHttpUtils.getInstance();
    }

    public Observable<ResponseBody> downloadFile(String fileUrl) {
        return DownloadRetrofit.downloadFile(fileUrl);
    }

    public Observable<ResponseBody> uploadImg(String uploadUrl, String filePath) {
        return UploadRetrofit.uploadImg(uploadUrl, filePath);
    }

    public Observable<ResponseBody> uploadImgs(String uploadUrl, List<String> filePaths) {
        return UploadRetrofit.uploadImgs(uploadUrl, filePaths);
    }

    public HashSet<String> getCookie() {
        HashSet<String> preferences = (HashSet<String>) SPUtils.get(context, SPUtils.FILE_NAME, SPConfig.COOKIE, new HashSet<String>());
        return preferences;
    }

    public void addDisposable(Disposable disposable) {
        if (disposables != null) {
            disposables.add(disposable);
        }
    }

    public void cancelAllRequest() {
        if (disposables != null) {
            for (Disposable disposable : disposables) {
                disposable.dispose();
            }
            disposables.clear();
        }
    }

    public void cancelSingleRequest(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
    }

    private static void checkInitialize() {
        if (context == null) {
            throw new ExceptionInInitializerError("请先在全局Application中调用 HttpUtils.init() 初始化！");
        }
    }
}
