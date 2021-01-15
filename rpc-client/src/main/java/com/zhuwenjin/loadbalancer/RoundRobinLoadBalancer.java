package com.zhuwenjin.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

/**
 * @author ziyang
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private int index = 0;

    @Override
    public String select(String rpcServiceName ,List<String> serviceUrlList) {
        if(index >= serviceUrlList.size()) {
            index %= serviceUrlList.size();
        }
        String targetServiceUrl = serviceUrlList.get(index++);
        System.out.println("使用轮询负载均衡算法获得服务（"+rpcServiceName+"）的地址："+targetServiceUrl);
        return targetServiceUrl;
    }

}
