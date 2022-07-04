package com.zhangteng.rxhttputils.exception;

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializer;
import com.zhangteng.utils.IException;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;

import java.io.IOException;
import java.io.NotSerializableException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Objects;

import retrofit2.HttpException;

public class ApiException extends IException {

    private String message;

    public ApiException(@Nullable String message) {
        super(message);
        this.message = message;
    }

    public ApiException(@Nullable String message, @Nullable Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    public ApiException(@Nullable Throwable cause) {
        super(cause);
        this.message = cause != null ? cause.getMessage() : null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public ApiException(@Nullable String message, @Nullable Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @NonNull
    @Override
    public IException handleException() {
        Throwable e = getCause();
        if (e instanceof HttpException) {
            HttpException httpException = (HttpException) e;
            try {
                setCode(httpException.code());
                message = Objects.requireNonNull(Objects.requireNonNull(httpException.response()).errorBody()).string();
            } catch (IOException e1) {
                e1.printStackTrace();
                message = e1.getMessage();
            }
            return this;
        } else if (e instanceof SocketTimeoutException) {
            setCode(ERROR.TIMEOUT_ERROR);
            message = "网络连接超时，请检查您的网络状态后重试！";
            return this;
        } else if (e instanceof ConnectException) {
            setCode(ERROR.TIMEOUT_ERROR);
            message = "网络连接异常，请检查您的网络状态后重试！";
            return this;
        } else if (e instanceof ConnectTimeoutException) {
            setCode(ERROR.TIMEOUT_ERROR);
            message = "网络连接超时，请检查您的网络状态后重试！";
            return this;
        } else if (e instanceof UnknownHostException) {
            setCode(ERROR.TIMEOUT_ERROR);
            message = "网络连接异常，请检查您的网络状态后重试！";
            return this;
        } else if (e instanceof NullPointerException) {
            setCode(ERROR.NULL_POINTER_EXCEPTION);
            message = "空指针异常";
            return this;
        } else if (e instanceof javax.net.ssl.SSLHandshakeException) {
            setCode(ERROR.SSL_ERROR);
            message = "证书验证失败";
            return this;
        } else if (e instanceof ClassCastException) {
            setCode(ERROR.CAST_ERROR);
            message = "类型转换错误";
            return this;
        } else if (e instanceof JsonParseException
                || e instanceof JSONException
                || e instanceof JsonSerializer
                || e instanceof NotSerializableException
                || e instanceof ParseException) {
            setCode(ERROR.PARSE_ERROR);
            message = "解析错误";
            return this;
        } else if (e instanceof IllegalStateException) {
            setCode(ERROR.ILLEGAL_STATE_ERROR);
            message = e.getMessage();
            return this;
        } else {
            setCode(ERROR.UNKNOWN);
            message = "未知错误";
            return this;
        }
    }

    public static IException handleException(Throwable e) {
        return new ApiException(e).handleException();
    }

    /**
     * 约定异常
     */
    public static class ERROR {
        /**
         * 未知错误
         */
        public static final int UNKNOWN = 1000;
        /**
         * 连接超时
         */
        public static final int TIMEOUT_ERROR = 1001;
        /**
         * 空指针错误
         */
        public static final int NULL_POINTER_EXCEPTION = 1002;

        /**
         * 证书出错
         */
        public static final int SSL_ERROR = 1003;

        /**
         * 类转换错误
         */
        public static final int CAST_ERROR = 1004;

        /**
         * 解析错误
         */
        public static final int PARSE_ERROR = 1005;

        /**
         * 非法数据异常
         */
        public static final int ILLEGAL_STATE_ERROR = 1006;

    }
}
