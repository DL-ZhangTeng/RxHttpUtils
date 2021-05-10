package com.zhangteng.app;

import java.io.Serializable;

public class LoginBean implements Serializable {
    private String token;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }
}
