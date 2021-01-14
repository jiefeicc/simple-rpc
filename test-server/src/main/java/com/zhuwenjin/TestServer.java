package com.zhuwenjin;

import com.zhuwenjin.netty.RpcServer;
import com.zhuwenjin.registry.DefaultServiceRegistry;
import com.zhuwenjin.registry.IServiceRegistry;
import com.zhuwenjin.serializer.ProtobufSerializer;


/**
 * 测试用Netty服务提供者（服务端）
 *
 * @author zhuwenjin
 */
public class TestServer {

    public static void main(String[] args) {
        HelloService helloService = new HelloServiceImpl();
        IServiceRegistry registry = new DefaultServiceRegistry();
        registry.register(helloService);
        RpcServer server = new RpcServer();
        server.setSerializer(new ProtobufSerializer());
        server.start(9999);
    }

}