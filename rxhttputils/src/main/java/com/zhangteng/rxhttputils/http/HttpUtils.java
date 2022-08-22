package com.zhangteng.rxhttputils.http;

import android.app.Application;
import android.content.Context;

import com.zhangteng.rxhttputils.config.SPConfig;
import com.zhangteng.rxhttputils.fileload.download.DownloadRetrofit;
import com.zhangteng.rxhttputils.fileload.upload.UploadRetrofit;
import com.zhangteng.utils.SPUtilsKt;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import io.reactivex.disposables.Disposable;

/**
 * Created by swing on 2018/4/24.
 */
public class HttpUtils {
    private static volatile HttpUtils instance;
    private static Application context;
    private static HashMap<Disposable, Object> disposables;

    private HttpUtils() {
        disposables = new HashMap<>();
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

    /**
     * description 初始化context
     */
    public static void init(Application app) {
        context = app;
    }

    /**
     * description 获取全局context
     */
    public Context getContext() {
        checkInitialize();
        return context;
    }

    /**
     * description 全局网络请求工具
     */
    public GlobalHttpUtils ConfigGlobalHttpUtils() {
        return GlobalHttpUtils.getInstance();
    }

    /**
     * description 单个网络请求工具
     */
    public SingleHttpUtils ConfigSingleInstance() {
        return SingleHttpUtils.getInstance();
    }

    /**
     * description 上传请求工具
     */
    public UploadRetrofit UploadRetrofit() {
        return UploadRetrofit.getInstance();
    }

    /**
     * description 下载请求工具
     */
    public DownloadRetrofit DownloadRetrofit() {
        return DownloadRetrofit.getInstance();
    }

    public HashSet<String> getCookie() {
        return (HashSet<String>) SPUtilsKt.getFromSPToSet(context, SPConfig.FILE_NAME, SPConfig.COOKIE, new HashSet<>());
    }

    /**
     * description 添加可处理对象集合
     *
     * @param disposable 可取消的对象
     */
    public void addDisposable(Disposable disposable) {
        if (disposables != null) {
            disposables.put(disposable, null);
        }
    }

    /**
     * description 添加可处理对象集合，可使用标记tag取消请求，配合生命周期监听可以在页面销毁时自动取消全部请求
     *
     * @param disposable 可取消的对象
     * @param tag        标记
     */
    public void addDisposable(Disposable disposable, Object tag) {
        if (disposables != null) {
            disposables.put(disposable, tag);
        }
    }

    /**
     * description 清除所有请求
     */
    public void cancelAllRequest() {
        if (disposables != null) {
            for (Disposable disposable : disposables.keySet()) {
                disposable.dispose();
            }
            disposables.clear();
        }
    }

    /**
     * description 取消单个请求，且单个请求结束后需要移除disposables时也可使用本方法
     *
     * @param disposable 可取消的对象
     */
    public void cancelSingleRequest(Disposable disposable) {
        if (disposable != null && !disposable.isDisposed()) {
            disposable.dispose();
        }
        if (disposables != null) {
            disposables.remove(disposable);
        }
    }

    /**
     * @param tag 单个请求的标识（推荐使用Activity/Fragment.this）,多个请求可以使用同一个tag，取消请求时会同时取消
     * @description 通过tag取消请求，tag可以通过CommonObserver/LifecycleObservableTransformer创建时传入
     */
    public void cancelSingleRequest(Object tag) {
        if (tag != null && disposables != null) {
            Set<Map.Entry<Disposable, Object>> set = disposables.entrySet();

            Iterator<Map.Entry<Disposable, Object>> iterator = set.iterator();

            while (iterator.hasNext()) {
                Map.Entry<Disposable, Object> entry = iterator.next();
                Disposable disposable = entry.getKey();

                if (tag.equals(entry.getValue()) && !disposable.isDisposed()) {
                    disposable.dispose();
                }

                if (disposable.isDisposed()) {
                    iterator.remove();
                }
            }
        }
    }

    private static void checkInitialize() {
        if (context == null) {
            throw new ExceptionInInitializerError("请先在全局Application中调用 HttpUtils.init() 初始化！");
        }
    }
}
