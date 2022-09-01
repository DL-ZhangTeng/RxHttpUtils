package com.zhangteng.rxhttputils.interceptor;

import com.zhangteng.rxhttputils.config.SPConfig;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.utils.SPUtilsKt;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import okhttp3.Interceptor;
import okhttp3.Response;

import static java.util.Calendar.getInstance;

/**
 * Created by swing on 2018/4/24.
 */
public class SaveCookieInterceptor implements Interceptor, PriorityInterceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response originalResponse = chain.proceed(chain.request());
        //这里获取请求返回的cookie
        if (!originalResponse.headers("Set-Cookie").isEmpty()) {
            HashSet<String> cookies = new HashSet<>();

            for (String header : originalResponse.headers("Set-Cookie")) {
                cookies.add(header);
            }
            SPUtilsKt.putToSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, SPConfig.COOKIE, cookies);
        }
        //获取服务器相应时间--用于计算倒计时的时间差
        if (!originalResponse.header("Date").isEmpty()) {
            long date = dateToStamp(originalResponse.header("Date"));
            SPUtilsKt.putToSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, SPConfig.DATE, date);
        }

        return originalResponse;
    }

    @Override
    public int getPriority() {
        return 2;
    }

    /**
     * 将时间转换为时间戳
     *
     * @param s date
     * @return long
     * @throws android.net.ParseException
     */
    public static long dateToStamp(String s) throws android.net.ParseException {
        //转换为标准时间对象
        Date date = new Date(s);
        Calendar calendar = getInstance();
        calendar.setTime(date);
        long mTimeInMillis = calendar.getTimeInMillis();
        return mTimeInMillis;
    }
}
