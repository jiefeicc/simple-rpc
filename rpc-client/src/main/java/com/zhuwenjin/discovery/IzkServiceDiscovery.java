package com.zhuwenjin.discovery;

import com.zhuwenjin.extension.SPI;

import java.net.InetSocketAddress;

/**
 * service discovery
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@SPI
public interface IzkServiceDiscovery {
    /**
     * lookup service by rpcServiceName
     *
     * @param rpcServiceName rpc service name
     * @return service address
     */
    InetSocketAddress lookupService(String rpcServiceName);
}
