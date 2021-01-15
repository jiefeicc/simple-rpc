package com.zhuwenjin;


import com.zhuwenjin.netty.RpcClient;
import com.zhuwenjin.serializer.CommonSerializer;

/**
 * 测试用Netty消费者
 *
 * @author ziyang
 */
public class TestClient {

    public static void main(String[] args) {
        IRpcClient client = new RpcClient(CommonSerializer.PROTOBUF_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        System.out.println(res);
    }

}
