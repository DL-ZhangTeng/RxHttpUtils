# RxJava+Retrofit网络加载库二次封装-RxHttpUtils
RxHttpUtils是RxJava+Retrofit网络加载库二次封装，包含网络加载动画、activity销毁自动取消请求、网络缓存、公共参数、RSA+AES加密等
[GitHub仓库地址](https://github.com/DL-ZhangTeng/RxHttpUtils)
## 引入

### gradle
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

implementation 'com.github.DL-ZhangTeng:RxHttpUtils:1.2.1'
//库所使用的三方
implementation 'androidx.lifecycle:lifecycle-common:2.3.1'
implementation 'androidx.lifecycle:lifecycle-runtime:2.3.1'
implementation "io.reactivex.rxjava2:rxjava:2.2.21"
implementation 'io.reactivex.rxjava2:rxandroid:2.1.1'
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.retrofit2:converter-scalars:2.8.1'
implementation 'com.squareup.retrofit2:adapter-rxjava2:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:5.0.0-alpha.2'
implementation 'com.github.DL-ZhangTeng.BaseLibrary:utils:1.3.5'
```

## 属性
属性名| 描述
--- | -----
setBaseUrl| ConfigGlobalHttpUtils()全局的BaseUrl；ConfigSingleInstance()单独设置BaseUrl
setCache| 开启缓存策略
setHeaders| 全局的请求头信息，设置静态请求头：更新请求头时不需要重新设置，对Map元素进行移除添加即可；设置动态请求头：如token等需要根据登录状态实时变化的请求头参数，最小支持api 24
setSign| 全局验签，appKey与后端匹配即可，具体规则参考：https://blog.csdn.net/duoluo9/article/details/105214983
setEnAndDecryption| 全局加解密(AES+RSA)。1、公钥请求路径HttpUrl.get(BuildConfig.HOST + "/getPublicKey")；2、公钥响应结果{"result": {"publicKey": ""},"message": "查询成功!","status": 100}
setCookie|全局持久话cookie,保存本地每次都会携带在header中
setSslSocketFactory| 全局ssl证书认证。1、信任所有证书,不安全有风险，setSslSocketFactory()；2、使用预埋证书，校验服务端证书（自签名证书），setSslSocketFactory(getAssets().open("your.cer"))；3、使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书），setSslSocketFactory(getAssets().open("your.bks"), "123456", getAssets().open("your.cer"))
setReadTimeOut|全局超时配置
setWriteTimeOut|全局超时配置
setConnectionTimeOut| 全局超时配置
setLog| 全局是否打开请求log日志
## 使用
```java
 public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        HttpUtils.init(this);
        HttpUtils.getInstance()
                .ConfigGlobalHttpUtils()
                //全局的BaseUrl
                .setBaseUrl("http://**/")
                //开启缓存策略
                .setCache(true)
                //全局的静态请求头信息
                //.setHeaders(headersMap)
                //全局的请求头信息
                //.setHeaders(headersMap, headers -> {
                //  if (headers == null) {
                //      headers = new HashMap<>();
                //  }
                //  boolean isLogin = BuildConfig.DEBUG;
                //  if (isLogin) {
                //      headers.put("Authorization", "Bearer " + "token");
                //  } else {
                //      headers.remove("Authorization");
                //  }
                //  return headers;
                //})
                //全局的动态请求头信息
                .setHeaders(headers -> {
                    if (headers == null) {
                        headers = new HashMap<>();
                    }
                    headers.put("version", BuildConfig.VERSION_CODE);
                    headers.put("os", "android");

                    boolean isLogin = BuildConfig.DEBUG;
                    if (isLogin) {
                        headers.put("Authorization", "Bearer " + "token");
                    } else {
                        headers.remove("Authorization");
                    }
                    return headers;
                })
                //全局持久话cookie,保存本地每次都会携带在header中
                .setCookie(false)
                //全局ssl证书认证
                //信任所有证书,不安全有风险
                .setSslSocketFactory()
                //使用预埋证书，校验服务端证书（自签名证书）
                //.setSslSocketFactory(getAssets().open("your.cer"))
                //使用bks证书和密码管理客户端证书（双向认证），使用预埋证书，校验服务端证书（自签名证书）
                //.setSslSocketFactory(getAssets().open("your.bks"), "123456", getAssets().open("your.cer"))
                //全局超时配置
                .setReadTimeOut(10)
                //全局超时配置
                .setWriteTimeOut(10)
                //全局超时配置
                .setConnectionTimeOut(10)
                //全局是否打开请求log日志
                .setLog(true);
    }
}
```

```java
//使用生命周期监听自动取消请求、加载中动画自动处理（LifecycleObservableTransformer、ProgressDialogObservableTransformer）
 HttpUtils.getInstance()
                .ConfigGlobalHttpUtils()
                .createService(ApiService.class)
                .loginPwd("admin", "admin")
                .compose(new LifecycleObservableTransformer<>(MainActivity.this))
                .compose(new ProgressDialogObservableTransformer<>(mProgressDialog))
                .subscribe(new BaseObserver<BaseResponse<LoginBean>>() {
                    @Override
                    public void doOnSubscribe(Disposable d) {

                    }

                    @Override
                    public void doOnError(String errorMsg) {

                    }

                    @Override
                    public void doOnNext(BaseResponse<LoginBean> loginBeanBaseResponse) {
                        ToastUtils.show(MainActivity.this, loginBeanBaseResponse.getMsg(), Toast.LENGTH_LONG);
                    }

                    @Override
                    public void doOnCompleted() {

                    }
                });
                
//使用生命周期监听自动取消请求、加载中动画自动处理（CommonObserver方案）
//        HttpUtils.getInstance()
//                .ConfigGlobalHttpUtils()
//                .createService(ApiService.class)
//                .loginPwd("admin", "admin")
//                .subscribe(new CommonObserver<BaseResponse<LoginBean>>() {
//                    @Override
//                    protected void onFailure(String errorMsg) {
//
//                    }
//
//                    @Override
//                    protected void onSuccess(BaseResponse<LoginBean> loginBeanBaseResponse) {
//                        ToastUtils.show(MainActivity.this, loginBeanBaseResponse.getMsg(), Toast.LENGTH_LONG);
//                    }
//                });

//手动取消网络请求
//        HttpUtils.getInstance().cancelSingleRequest(this);
//        HttpUtils.getInstance().cancelSingleRequest(Disposable);
//        HttpUtils.getInstance().cancelAllRequest();
```

## 混淆
-keep public class com.zhangteng.**.*{ *; }
## 历史版本
版本| 更新| 更新时间
-------- | ----- | -----
v1.2.1|增加动态请求头添加方法|2022/6/25 at 16:46
v1.2.0|使用base库utils|2022/1/21 at 20:14
v1.1.9|ConcurrentModificationException报错|2022/1/2 at 21:28
v1.1.8|下载文件配置|2021/12/21 at 23:42
v1.1.7|文件上传支持自定义字段名|2021/10/28 at 17:06
v1.1.6|请求取消方案修改：1、Lifecycle生命周期监听自动取消请求增加ObservableTransformer方案；2、请求结束自动从集合移除；3、页面销毁自动移除所有Tag对应的请求|2021/10/9 at 15:38
v1.1.5|1.增加Lifecycle生命周期监听自动取消请求；2.增加通过tag取消请求 |2021/9/22 at 16:43
v1.1.4| 1.createService时全部使用RetrofitServiceProxyHandler;2.上传下载单例bug|2021/7/15 at 15:36
v1.1.3| 单接口配置与全局配置同步方法&支持UnitTest|2021/7/12 at 17:45
v1.1.2| 增加Service的lru缓存|2021/5/10 at 16:25
v1.1.1| 加解密失败构建响应bug|2020/12/14 at 18:04
v1.1.0| 迁移到androidx|2020/7/22 at 13:40
v1.0.3| 全局拦截器失效bug|2020/6/27 at 12:22
v1.0.2| 增加权限，修改模块名为小写|2020/6/11 at 9:45
v1.0.1| 包结构调整|2020/6/11 at 9:36
v1.0.0| 初版| 2020/6/3 at 17:13

## 赞赏
如果您喜欢RxHttpUtils，或感觉RxHttpUtils帮助到了您，可以点右上角“Star”支持一下，您的支持就是我的动力，谢谢

## 联系我
邮箱：763263311@qq.com/ztxiaoran@foxmail.com

## License
Copyright (c) [2020] [Swing]

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
