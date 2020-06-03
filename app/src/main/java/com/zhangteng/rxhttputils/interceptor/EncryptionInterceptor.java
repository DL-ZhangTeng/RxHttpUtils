package com.zhangteng.rxhttputils.interceptor;

import android.text.TextUtils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zhangteng.rxhttputils.http.HttpUtils;
import com.zhangteng.rxhttputils.http.OkHttpClient;
import com.zhangteng.rxhttputils.utils.AESUtils;
import com.zhangteng.rxhttputils.utils.DiskLruCacheUtils;
import com.zhangteng.rxhttputils.utils.RSAUtils;
import com.zhangteng.rxhttputils.utils.SPUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
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
    public static final String SECRET = "_secret";
    public static final int SECRET_ERROR = 2100;
    public static final String publicKey = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCdxW9Vuhso76iIyY8IBHJHXGv7Jzva95I8KqBY+MC3OV4HHY5vUG84NgXvlbB4eyyYxeAQQ16Mp30xJ1tKYZmVUQalIpjZUi9rYgf/zBwJlbRP9DtNzdCdjcXxStY4oE5/jYMrsPtX6K26gkxlsXUfpSV5Lv0Q/OnRYztLn2zZ7wIDAQAB";
    private HttpUrl publicKeyUrl;

    public EncryptionInterceptor(HttpUrl publicKeyUrl) {
        this.publicKeyUrl = publicKeyUrl;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        okhttp3.Request request = chain.request();
        Headers headers = request.headers();
        if (headers.names().contains(SECRET) && "true".equals(headers.get(SECRET))) {
            Request secretRequest = buildRequest(request);
            if (secretRequest == null) {
                return getErrorSecretResponse();
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
                    && String.valueOf(SECRET_ERROR).equals(jsonElement.getAsString())
            ) {
                SPUtils.put(HttpUtils.getInstance().getContext(), SPUtils.FILE_NAME, SECRET, "");
                DiskLruCacheUtils.remove(publicKeyUrl);
                DiskLruCacheUtils.flush();
                secretRequest = buildRequest(request);
                if (secretRequest == null) {
                    return getErrorSecretResponse();
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
    private Request buildRequest(Request request) throws IOException {
        if (TextUtils.isEmpty((CharSequence) SPUtils.get(HttpUtils.getInstance().getContext(), SPUtils.FILE_NAME, SECRET, ""))) {
            Response secretResponse = OkHttpClient.getInstance().getClient().newCall(new Request.Builder().url(publicKeyUrl).build()).execute();
            String secretResponseString = secretResponse.body().string();
            if (secretResponse.code() == 200) {
                JsonObject jsonObject = new JsonParser().parse(secretResponseString).getAsJsonObject();
                JsonElement jsonElement = jsonObject.get("result").getAsJsonObject().get("publicKey");
                SPUtils.put(HttpUtils.getInstance().getContext(), SPUtils.FILE_NAME, SECRET, jsonElement.getAsString());
            } else {
                return null;
            }
        }
        String aesRequestKey = AESUtils.getKey();
        okhttp3.Request.Builder requestBuilder = request.newBuilder();
        requestBuilder.removeHeader(SECRET);
        try {
            requestBuilder.addHeader(SECRET, RSAUtils.encryptByPublicKey(aesRequestKey, (String) SPUtils.get(HttpUtils.getInstance().getContext(), SPUtils.FILE_NAME, SECRET, publicKey)));
        } catch (Exception e) {
            return null;
        }
        if (METHOD_GET.equals(request.method())) {
            String url = request.url().url().toString();
            String paramsBuilder = url.substring(url.indexOf("?") + 1);
            try {
                String encryptParams = AESUtils.encrypt(paramsBuilder, aesRequestKey, aesRequestKey.substring(0, 16));
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
                                    String encryptParams = AESUtils.encrypt(value, aesRequestKey, aesRequestKey.substring(0, 16));
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
                            String encryptParams = AESUtils.encrypt(paramsRaw, aesRequestKey, aesRequestKey.substring(0, 16));
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
    private Response getErrorSecretResponse() {
        Response.Builder failureResponse = new Response.Builder();
        failureResponse.body(ResponseBody.create(MediaType.parse("application/json;charset=UTF-8"), String.format("{\"message\": \"移动端加密失败\",\"status\": %s}", EncryptionInterceptor.SECRET_ERROR)));
        return failureResponse.build();
    }
}
