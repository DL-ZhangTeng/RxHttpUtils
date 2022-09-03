package com.zhangteng.app;

import io.reactivex.Observable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    /**
     * 密码登录
     */
    @POST("/")
    @FormUrlEncoded
    Observable<BaseResponse<LoginBean>> loginPwd(@Field("username") String phone, @Field("password") String pwd);
}
