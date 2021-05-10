package com.zhangteng.app;

import java.io.Serializable;

public class BaseResponse<T> implements Serializable {
    private T data;
    private int code;
    private String msg;

    public T getData() {
        return data;
    }

    public String getMsg() {
        return msg;
    }

    public void setData(T data) {
        this.data = data;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
