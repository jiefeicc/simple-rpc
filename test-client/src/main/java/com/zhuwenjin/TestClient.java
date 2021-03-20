package com.zhuwenjin;


import com.zhuwenjin.netty.RpcClient;
import com.zhuwenjin.serializer.CommonSerializer;

/**
 * 测试用Netty消费者
 *
 * @author zhuwenjin
 */
public class TestClient {

    public static void main(String[] args) {
        IRpcClient client = new RpcClient(CommonSerializer.PROTOBUF_SERIALIZER);
        RpcClientProxy rpcClientProxy = new RpcClientProxy(client);
        // 在客户端这一侧我们并没有接口的具体实现类，就没有办法直接生成实例对象。
        // 这时，我们可以通过动态代理的方式生成实例，并且调用方法时生成需要的RpcRequest对象并且发送给服务端。
        HelloService helloService = rpcClientProxy.getProxy(HelloService.class);
        ByeService byeService = rpcClientProxy.getProxy(ByeService.class);

        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        System.out.println(res);
        System.out.println(byeService.bye("Netty"));
    }

}
