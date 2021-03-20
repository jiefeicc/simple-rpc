package com.zhuwenjin;

import com.zhuwenjin.annotation.ServiceScan;
import com.zhuwenjin.netty.RpcServer;
import com.zhuwenjin.serializer.CommonSerializer;


/**
 * 测试用Netty服务提供者（服务端）
 *
 * @author zhuwenjin
 */
@ServiceScan
// @ServiceScan 放在启动的入口类上（main 方法所在的类），标识服务的扫描的包的范围。
// @ServiceScan 的值定义为扫描范围的根包，默认值为入口类所在的包，
// 扫描时会扫描该包及其子包下所有的类，找到标记有 Service 的类，并注册。
public class TestServer {

    public static void main(String[] args) {
        RpcServer server = new RpcServer("127.0.0.1", 9999, CommonSerializer.PROTOBUF_SERIALIZER);
        server.start();
    }

}