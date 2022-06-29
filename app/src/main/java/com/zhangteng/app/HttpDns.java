package com.zhangteng.app;

import static java.net.InetAddress.getAllByName;

import androidx.annotation.NonNull;

import com.alibaba.sdk.android.httpdns.HttpDnsService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

/**
 * description: 阿里云HttpDNS
 * author: Swing 763263311@qq.com
 * date: 2020/12/16 0016 下午 17:56
 */
public class HttpDns implements Dns {
    private final HttpDnsService httpDns;

    private static volatile HttpDns instance;

    public static HttpDns getInstance() {
        if (instance == null) {
            synchronized (HttpDns.class) {
                if (instance == null) {
                    instance = new HttpDns();
                }
            }
        }
        return instance;
    }

    private HttpDns() {
        // 参数applicationContext是您Android App的Context
        // 参数accountID是系统分配的Account ID，当您开通HTTPDNS后，您可以在控制台获取到您对应的Account ID信息
        // 参数secretKey是鉴权对应的secretKey
        httpDns = com.alibaba.sdk.android.httpdns.HttpDns.getService(MainApplication.getInstance(), "159647", "39fd63b900bd5e49f21f262d939b72e2");
        ArrayList<String> hostList = new ArrayList<>(Arrays.asList("http://api.fengduoyun.com", "http://www.fengduoyun.com", "https://update.fengduoyun.com"));
        httpDns.setPreResolveHosts(hostList);
    }

    @Override
    public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
        if (httpDns == null)  //当构造失败时使用默认解析方式
            return Dns.SYSTEM.lookup(hostname);

        try {
            String[] ips = httpDns.getIpsByHostAsync(hostname);  //获取HttpDNS解析结果
            if (ips == null || ips.length == 0) {
                return Dns.SYSTEM.lookup(hostname);
            }

            List<InetAddress> result = new ArrayList<>();
            for (String ip : ips) {  //将ip地址数组转换成所需要的对象列表
                result.addAll(Arrays.asList(getAllByName(ip)));
            }
            //在返回result之前，我们可以添加一些其他自己知道的IP
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        }
        //当有异常发生时，使用默认解析
        return Dns.SYSTEM.lookup(hostname);
    }
}