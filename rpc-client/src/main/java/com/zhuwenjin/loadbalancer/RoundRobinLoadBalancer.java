package com.zhuwenjin.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

/**
 * @author ziyang
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    private int index = 0;

    @Override
    public String select(List<String> serviceUrlList) {
        if(index >= serviceUrlList.size()) {
            index %= serviceUrlList.size();
        }
        return serviceUrlList.get(index++);
    }

}
