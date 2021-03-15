package com.zhuwenjin.registry;

import com.zhuwenjin.util.CuratorUtils;
import lombok.extern.slf4j.Slf4j;


import java.net.InetSocketAddress;

/**
 * service registration  based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 10:56:00
 */
@Slf4j
public class ZkServiceRegistry implements IzkServiceRegistry {
    //baseSleepTimeMs：重试之间等待的初始时间 maxRetries ：最大重试次数
    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;

    // Retry strategy. Retry 3 times, and will increase the sleep time between retries.

    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorUtils.createPersistentNode(servicePath);
        System.out.println("向zk注冊服务（"+servicePath+"）");
    }
}
