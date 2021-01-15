package com.zhuwenjin.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;
import java.util.Random;

/**
 * @author ziyang
 */
public class RandomLoadBalancer implements LoadBalancer {

    @Override
    public String select(List<String> serviceUrlList) {
        String targetServiceUrl = serviceUrlList.get(new Random().nextInt(serviceUrlList.size()));
        System.out.println("使用随即负载均衡算法获得服务地址："+targetServiceUrl);
        return targetServiceUrl;
    }

}
