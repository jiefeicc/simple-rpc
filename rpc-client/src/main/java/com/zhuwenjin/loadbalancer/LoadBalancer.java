package com.zhuwenjin.loadbalancer;

import com.alibaba.nacos.api.naming.pojo.Instance;

import java.util.List;

/**
 * @author zhuwenjin
 */
public interface LoadBalancer {

    String select(String rpcServiceName , List<String> serviceUrlList);

}
