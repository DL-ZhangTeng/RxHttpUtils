package com.zhangteng.rxhttputils.interceptor;

import android.text.TextUtils;

import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.utils.AESUtils;
import com.zhangteng.rxhttputils.utils.RSAUtils;
import com.zhangteng.rxhttputils.utils.SPUtils;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 添加加解密拦截器
 * Created by Swing on 2019/10/20.
 */
public class DecryptionInterceptor implements Interceptor {
    @Override
    public Response intercept(Chain chain) throws IOException {
        Response response = chain.proceed(chain.request());
        if (!response.isSuccessful() || response.code() != 200) {
            return response;
        }
        Response.Builder responseBuilder = response.newBuilder();
        ResponseBody responseBody = response.body();
        Headers responseHeaders = response.headers();
        for (String name : responseHeaders.names()) {
            if (EncryptionInterceptor.SECRET.contains(name) && !TextUtils.isEmpty(responseHeaders.get(name))) {
                try {
                    String encryptKey = responseHeaders.get(name);
                    String aesResponseKey = RSAUtils.decryptByPublicKey(encryptKey, (String) SPUtils.get(HttpUtils.getInstance().getContext(), SPUtils.FILE_NAME, EncryptionInterceptor.SECRET, EncryptionInterceptor.publicKey));
                    MediaType mediaType = responseBody != null ? responseBody.contentType() : MediaType.parse("application/json;charset=UTF-8");
                    String responseStr = responseBody != null ? responseBody.string() : "";
                    String rawResponseStr = AESUtils.decrypt(responseStr, aesResponseKey, aesResponseKey.substring(0, 16));
                    responseBuilder.body(ResponseBody.create(mediaType, rawResponseStr));
                    return responseBuilder.build();
                } catch (Exception e) {
                    Response.Builder failureResponse = new Response.Builder();
                    failureResponse.body(ResponseBody.create(MediaType.parse("application/json;charset=UTF-8"), String.format("{\"message\": \"移动端解密失败%s\",\"status\": %s}", e.getMessage(), EncryptionInterceptor.SECRET_ERROR)));
                    return failureResponse.build();
                }
            }
        }
        return response;
    }
}
