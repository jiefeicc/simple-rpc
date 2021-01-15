package com.zhuwenjin;

import com.zhuwenjin.netty.RpcServer;
import com.zhuwenjin.serializer.CommonSerializer;


/**
 * 测试用Netty服务提供者（服务端）
 *
 * @author zhuwenjin
 */
public class TestServer {

    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        RpcServer server = new RpcServer("127.0.0.1", 9999, CommonSerializer.PROTOBUF_SERIALIZER);
        server.publishService(helloService, HelloService.class);
    }

}