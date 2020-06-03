package com.zhangteng.rxhttputils.interceptor;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zhangteng.rxhttputils.utils.MD5Util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;

/**
 * 添加签名拦截器
 * Created by Swing on 2019/10/20.
 */
public class SignInterceptor implements Interceptor {
    private static final String METHOD_GET = "GET";
    private static final String METHOD_POST = "POST";
    private String appKey;

    public SignInterceptor(String appKey) {
        this.appKey = appKey;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Request.Builder requestBuilder = request.newBuilder();
        HttpUrl.Builder urlBuilder = request.url().newBuilder();
        Map<String, Object> params = new TreeMap<>();
        if (METHOD_GET.equals(request.method())) {
            HttpUrl httpUrl = urlBuilder.build();
            Set<String> paramKeys = httpUrl.queryParameterNames();
            for (String key : paramKeys) {
                String value = httpUrl.queryParameter(key);
                if (!TextUtils.isEmpty(value))
                    params.put(key, value);
            }
        } else if (METHOD_POST.equals(request.method())) {
            if (request.body() instanceof FormBody) {
                FormBody formBody = (FormBody) request.body();
                for (int i = 0; i < formBody.size(); i++) {
                    params.put(formBody.encodedName(i), formBody.encodedValue(i));
                }
            } else if (request.body() instanceof RequestBody) {
                RequestBody requestBody = request.body();
                Buffer buffer = new Buffer();
                requestBody.writeTo(buffer);
                Charset charset = StandardCharsets.UTF_8;
                MediaType contentType = requestBody.contentType();
                if (contentType != null) {
                    charset = contentType.charset();
                }
                String paramJson = buffer.readString(charset != null ? charset : Charset.defaultCharset());
                JsonObject jsonObject = new JsonParser().parse(paramJson).getAsJsonObject();
                for (String key : jsonObject.keySet()) {
                    JsonElement jsonElement = jsonObject.get(key);
                    if (jsonElement != null && !jsonElement.isJsonArray() && !jsonElement.isJsonObject() && !jsonElement.isJsonNull()) {
                        String value = jsonElement.getAsString();
                        if (!TextUtils.isEmpty(value))
                            params.put(key, value);
                    }
                }
            }
        }
        StringBuilder sign = new StringBuilder();
        sign.append(appKey);
        for (String key : params.keySet()) {
            sign.append(key).append(params.get(key));
        }
        long _timestamp = System.currentTimeMillis();
        sign.append("_timestamp").append(_timestamp);
        sign.append(appKey);
        requestBuilder.addHeader("_timestamp", String.valueOf(_timestamp));
        requestBuilder.addHeader("_sign", MD5Util.md5Decode32(sign.toString()));
        return chain.proceed(requestBuilder.build());
    }
}
