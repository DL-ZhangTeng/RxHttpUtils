package com.zhangteng.rxhttputils.interceptor;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zhangteng.rxhttputils.config.EncryptConfig;
import com.zhangteng.rxhttputils.config.SPConfig;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.http.OkHttpClient;
import com.zhangteng.rxhttputils.utils.DiskLruCacheUtils;
import com.zhangteng.utils.AESUtils;
import com.zhangteng.utils.RSAUtils;
import com.zhangteng.utils.SPUtilsKt;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

/**
 * 添加加解密拦截器
 * Created by Swing on 2019/10/20.
 */
public class EncryptionInterceptor implements Interceptor {
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";

    public EncryptionInterceptor() {

    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        Headers headers = request.headers();
        if (headers.names().contains(EncryptConfig.SECRET) && "true".equals(headers.get(EncryptConfig.SECRET))) {
            Request secretRequest = buildRequest(request);
            if (secretRequest == null) {
                return getErrorSecretResponse(request);
            }
            Response secretResponse = chain.proceed(secretRequest);
            ResponseBody secretResponseBody = secretResponse.body();
            String secretResponseStr = secretResponseBody != null ? secretResponseBody.string() : "";
            JsonObject jsonObject = new JsonParser().parse(
                    secretResponseStr.substring(
                            0,
                            secretResponseStr.lastIndexOf("}") + 1
                    )
            ).getAsJsonObject();
            JsonElement jsonElement = jsonObject.get("status");
            if (jsonElement != null
                    && !jsonElement.isJsonArray()
                    && !jsonElement.isJsonObject()
                    && !jsonElement.isJsonNull()
                    && String.valueOf(EncryptConfig.SECRET_ERROR).equals(jsonElement.getAsString())
            ) {
                SPUtilsKt.putToSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, EncryptConfig.SECRET, "");
                DiskLruCacheUtils.remove(EncryptConfig.publicKeyUrl);
                DiskLruCacheUtils.flush();
                secretRequest = buildRequest(request);
                if (secretRequest == null) {
                    return getErrorSecretResponse(request);
                }
                secretResponse = chain.proceed(secretRequest);
            } else {
                MediaType mediaType = secretResponseBody != null ? secretResponseBody.contentType() : MediaType.parse("application/json;charset=UTF-8");
                ResponseBody newResonseBody = ResponseBody.create(mediaType, secretResponseStr);
                secretResponse = secretResponse.newBuilder().body(newResonseBody).build();
            }
            return secretResponse;
        }
        return chain.proceed(request);
    }

    /**
     * 构建加密请求
     *
     * @param request 原请求
     */
    protected Request buildRequest(Request request) throws IOException {
        if (TextUtils.isEmpty((CharSequence) SPUtilsKt.getFromSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, EncryptConfig.SECRET, ""))) {
            Response secretResponse = OkHttpClient.getInstance().getClient().newCall(new Request.Builder().url(EncryptConfig.publicKeyUrl).build()).execute();
            if (secretResponse.code() == 200) {
                try {
                    String secretResponseString = Objects.requireNonNull(secretResponse.body()).string();
                    JsonObject jsonObject = new JsonParser().parse(secretResponseString).getAsJsonObject();
                    JsonElement jsonElement = jsonObject.get("result").getAsJsonObject().get("publicKey");
                    SPUtilsKt.putToSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, EncryptConfig.SECRET, jsonElement.getAsString());
                } catch (NullPointerException exception) {
                    return null;
                }
            } else {
                return null;
            }
        }
        String aesRequestKey = AESUtils.INSTANCE.getKey();
        okhttp3.Request.Builder requestBuilder = request.newBuilder();
        requestBuilder.removeHeader(EncryptConfig.SECRET);
        try {
            requestBuilder.addHeader(EncryptConfig.SECRET, RSAUtils.INSTANCE.encryptByPublicKey(aesRequestKey, (String) SPUtilsKt.getFromSP(HttpUtils.getInstance().getContext(), SPConfig.FILE_NAME, EncryptConfig.SECRET, EncryptConfig.publicKey)));
        } catch (Exception e) {
            return null;
        }
        if (METHOD_GET.equals(request.method())) {
            String url = request.url().url().toString();
            String paramsBuilder = url.substring(url.indexOf("?") + 1);
            try {
                String encryptParams = AESUtils.INSTANCE.encrypt(paramsBuilder, aesRequestKey, aesRequestKey.substring(0, 16));
                requestBuilder.url(url.substring(0, url.indexOf("?")) + "?" + encryptParams);
            } catch (Exception e) {
                return null;
            }
        } else if (METHOD_POST.equals(request.method())) {
            RequestBody requestBody = request.body();
            if (requestBody != null && aesRequestKey.length() >= 16) {
                if (requestBody instanceof FormBody) {
                    FormBody formBody = (FormBody) request.body();
                    FormBody.Builder bodyBuilder = new FormBody.Builder();
                    try {
                        if (formBody != null) {
                            for (int i = 0; i < formBody.size(); i++) {
                                String value = formBody.encodedValue(i);
                                if (!TextUtils.isEmpty(value)) {
                                    String encryptParams = AESUtils.INSTANCE.encrypt(value, aesRequestKey, aesRequestKey.substring(0, 16));
                                    bodyBuilder.addEncoded(formBody.encodedName(i), encryptParams);
                                }
                            }
                            requestBuilder.post(bodyBuilder.build());
                        }
                    } catch (Exception e) {
                        return null;
                    }
                } else {
                    Buffer buffer = new Buffer();
                    requestBody.writeTo(buffer);
                    Charset charset = StandardCharsets.UTF_8;
                    MediaType contentType = requestBody.contentType();
                    if (contentType != null) {
                        charset = contentType.charset();
                    }
                    String paramsRaw = buffer.readString(charset != null ? charset : Charset.defaultCharset());
                    if (!TextUtils.isEmpty(paramsRaw)) {
                        try {
                            String encryptParams = AESUtils.INSTANCE.encrypt(paramsRaw, aesRequestKey, aesRequestKey.substring(0, 16));
                            requestBuilder.post(RequestBody.create(requestBody.contentType(), encryptParams));
                        } catch (Exception e) {
                            return null;
                        }
                    }
                }
            }
        }
        return requestBuilder.build();
    }

    /**
     * 获取加密失败响应
     */
    protected Response getErrorSecretResponse(okhttp3.Request request) {
        Response.Builder failureResponse = new Response.Builder();
        failureResponse.request(request);
        failureResponse.body(ResponseBody.create(MediaType.parse("application/json;charset=UTF-8"), String.format("{\"message\": \"移动端加密失败\",\"status\": %s}", EncryptConfig.SECRET_ERROR)));
        return failureResponse.build();
    }
}
