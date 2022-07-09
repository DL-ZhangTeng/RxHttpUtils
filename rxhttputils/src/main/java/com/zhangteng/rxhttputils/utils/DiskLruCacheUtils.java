package com.zhangteng.rxhttputils.utils;

import android.os.Environment;
import android.util.Log;

import com.zhangteng.rxhttputils.http.GlobalHttpUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import okhttp3.Cache;
import okhttp3.HttpUrl;
import okhttp3.internal.cache.DiskLruCache;
import okhttp3.internal.concurrent.TaskRunner;
import okhttp3.internal.io.FileSystem;
import okio.ByteString;

public class DiskLruCacheUtils {
    private static DiskLruCache mDiskLruCache;

    private static void createDiskLruCache() {
        Cache cache = GlobalHttpUtils.getInstance().getOkHttpClient().cache();
        Class<?> clazz = Cache.class;
        try {
            Field cacheField = clazz.getDeclaredField("cache");
            cacheField.setAccessible(true);
            mDiskLruCache = (DiskLruCache) cacheField.get(cache);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            File file = new File(Environment.getExternalStorageDirectory()
                    .toString() + "/RxHttpUtilsCache");
            if (!file.exists()) {
                file.mkdirs();
            }
            try {
                mDiskLruCache = DiskLruCache.class
                        .getDeclaredConstructor(FileSystem.class, File.class, Integer.class, Integer.class, Long.class, TaskRunner.class)
                        .newInstance(FileSystem.SYSTEM, file, 201105, 2, 10485760, TaskRunner.INSTANCE);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException | NoSuchMethodException ex) {
                ex.printStackTrace();
            }
        }
    }


    private static String key(String key) {
        return ByteString.encodeUtf8(key).md5().hex();
    }

    private static String key(HttpUrl key) {
        return ByteString.encodeUtf8(key.toString()).md5().hex();
    }

    /**
     * 从本地缓存中移除一条缓存
     *
     * @param imageUrl
     * @return
     */
    public static boolean remove(String imageUrl) {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        try {
            String key = key(imageUrl);
            mDiskLruCache.remove(key);
            mDiskLruCache = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DiskLruCacheUtils", "移除失败");
            mDiskLruCache = null;
        }
        return false;
    }

    /**
     * 从本地缓存中移除一条缓存
     *
     * @param imageUrl
     * @return
     */
    public static boolean remove(HttpUrl imageUrl) {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        try {
            String key = key(imageUrl);
            mDiskLruCache.remove(key);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DiskLruCacheUtils", "移除失败");
        }
        mDiskLruCache = null;
        return false;
    }

    /**
     * 获取当前本地缓存的大小
     *
     * @return
     */
    public static long getCacheSize() {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        long size = 0;
        try {
            size = mDiskLruCache.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mDiskLruCache = null;
        return size;
    }

    /**
     * 将内存中的操作记录同步到日志文件（也就是journal文件）当中
     * 建议Activity的onPause()方法中去调用一次
     */
    public static void flush() {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        try {
            mDiskLruCache.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DiskLruCacheUtils", "日志文件写入失败");
        }
        mDiskLruCache = null;
    }

    /**
     * 将DiskLruCache关闭掉(open()方法对应)<br/>
     * 关闭掉了之后就不能再调用DiskLruCache中任何操作缓存数据的方法<br/>
     * 建议在Activity的onDestroy()方法中去调用
     */
    public static void close() {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        try {
            mDiskLruCache.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DiskLruCacheUtils", "DiskLruCache关闭失败");
        }
        mDiskLruCache = null;
    }

    /**
     * 将所有的缓存数据全部删除<br/>
     */
    public static boolean evictAll() {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        try {
            mDiskLruCache.evictAll();
            mDiskLruCache = null;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            mDiskLruCache = null;
            Log.d("DiskLruCacheUtils", "本地缓存删除失败");
            return false;
        }
    }

    /**
     * 将所有的缓存数据全部删除并关闭<br/>
     */
    public static void delete() {
        if (mDiskLruCache == null) {
            createDiskLruCache();
        }
        try {
            mDiskLruCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("DiskLruCacheUtils", "本地缓存删除失败");
        }
        mDiskLruCache = null;
    }
}
