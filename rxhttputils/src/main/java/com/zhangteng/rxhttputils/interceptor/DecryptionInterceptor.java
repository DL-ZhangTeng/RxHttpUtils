package com.zhangteng.rxhttputils.interceptor;

import android.text.TextUtils;

import com.zhangteng.rxhttputils.config.EncryptConfig;
import com.zhangteng.rxhttputils.config.SPConfig;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.utils.AESUtils;
import com.zhangteng.utils.RSAUtils;
import com.zhangteng.utils.SPUtilsKt;

import java.io.IOException;

import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * 添加解密拦截器
 * Created by Swing on 2019/10/20.
 */
public class DecryptionInterceptor implements Interceptor, PriorityInterceptor {
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
            if (EncryptConfig.SECRET.contains(name) && !TextUtils.isEmpty(responseHeaders.get(name))) {
                try {
                    String encryptKey = responseHeaders.get(name);
                    String aesResponseKey = RSAUtils.INSTANCE.decryptByPublicKey(encryptKey, (String) SPUtilsKt.getFromSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, EncryptConfig.SECRET, EncryptConfig.publicKey));
                    MediaType mediaType = responseBody != null ? responseBody.contentType() : MediaType.parse("application/json;charset=UTF-8");
                    String responseStr = responseBody != null ? responseBody.string() : "";
                    String rawResponseStr = AESUtils.INSTANCE.decrypt(responseStr, aesResponseKey, aesResponseKey.substring(0, 16));
                    responseBuilder.body(ResponseBody.create(mediaType, rawResponseStr));
                    return responseBuilder.build();
                } catch (Exception e) {
                    responseBuilder.body(getErrorSecretResponse(e));
                    return responseBuilder.build();
                }
            }
        }
        return response;
    }

    /**
     * description 保证解密时优先执行且避免特殊情况需要早于解密之前执行的NetworkInterceptor因此返回Integer.MAX_VALUE - 1
     */
    @Override
    public int getPriority() {
        return Integer.MAX_VALUE - 1;
    }

    /**
     * 获取解密失败响应
     */
    protected ResponseBody getErrorSecretResponse(Exception e) {
        return ResponseBody.create(MediaType.parse("application/json;charset=UTF-8"), String.format("{\"message\": \"移动端解密失败%s\",\"status\": %s}", e.getMessage(), EncryptConfig.SECRET_ERROR));
    }
}
