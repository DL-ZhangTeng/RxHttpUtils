package com.zhangteng.app;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiService {
    /**
     * 密码登录
     */
    @POST("app/login/pwd")
    @FormUrlEncoded
    Observable<BaseResponse<LoginBean>> loginPwd(@Field("username") String phone, @Field("password") String pwd);
}
