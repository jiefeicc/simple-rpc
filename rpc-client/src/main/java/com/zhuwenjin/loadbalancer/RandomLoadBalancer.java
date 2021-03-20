package com.zhuwenjin.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Random;

/**
 * @author zhuwenjin
 */
public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public String select(String rpcServiceName , List<String> serviceUrlList) {
        String targetServiceUrl = serviceUrlList.get(new Random().nextInt(serviceUrlList.size()));
        System.out.println("使用随机负载均衡算法获得服务（"+rpcServiceName+"）的地址："+targetServiceUrl);
        return targetServiceUrl;
    }

}
