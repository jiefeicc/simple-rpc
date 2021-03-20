## **特性**

- 实现了基于Netty的异步非阻塞网络传输方式 
- 实现了多种序列化算法：Kryo 算法、Hessian 算法与Protobuf 方式（默认采用 Kryo方式）
- 实现了两种负载均衡算法：随机算法与轮转算法（默认使用轮转算法） 
- 使用 Zookeeper作为注册中心，管理服务提供者信息 
- 服务提供侧自动注册服务 
- 实现自定义的通信协议 
- 利用钩子函数自动注销服务

## 项目模块

- **rpc-common** —— 实体对象、工具类等公用类
- **rpc-server** —— 框架的核心实现（服务提供端）
- **rpc-client** —— 框架的核心实现（服务消费端）
- **test-api** —— 测试用通用接口
- **test-client** —— 测试用消费侧
- **test-server** —— 测试用提供侧

## 自定义传输协议

调用参数与返回值的传输采用了如下协议，以防止粘包：

```
+---------------+---------------+-----------------+-------------+
|  Magic Number |  Package Type | Serializer Type | Data Length |
|    4 bytes    |    4 bytes    |     4 bytes     |   4 bytes   |
+---------------+---------------+-----------------+-------------+
|                          Data Bytes                           |
|                   Length: ${Data Length}                      |
+---------------------------------------------------------------+
```

| 字段            | 解释                                                         |
| --------------- | ------------------------------------------------------------ |
| Magic Number    | 魔数，表识一个协议包，0xCAFEBABE                             |
| Package Type    | 包类型，标明这是一个调用请求还是调用响应                     |
| Serializer Type | 序列化器类型，标明这个包的数据的序列化方式                   |
| Data Length     | 数据字节的长度                                               |
| Data Bytes      | 传输的对象，通常是一个`RpcRequest`或`RpcClient`对象，取决于`Package Type`字段，对象的序列化方式取决于`Serializer Type`字段。 |

**编解码代码如下**

```java
public class CommonDecoder extends ReplayingDecoder {

    private static final Logger logger = LoggerFactory.getLogger(CommonDecoder.class);
    // 魔数，表识一个协议包
    private static final int MAGIC_NUMBER = 0xCAFEBABE;

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        int magic = in.readInt();
        if (magic != MAGIC_NUMBER) {
            logger.error("不识别的协议包: {}", magic);
            throw new RpcException(RpcError.UNKNOWN_PROTOCOL);
        }
        int packageCode = in.readInt();
        Class<?> packageClass;
        // 包类型，标明这是一个调用请求还是调用响应
        if (packageCode == PackageType.REQUEST_PACK.getCode()) {
            packageClass = RpcRequest.class;
        } else if (packageCode == PackageType.RESPONSE_PACK.getCode()) {
            packageClass = RpcResponse.class;
        } else {
            logger.error("不识别的数据包: {}", packageCode);
            throw new RpcException(RpcError.UNKNOWN_PACKAGE_TYPE);
        }
        int serializerCode = in.readInt();
        CommonSerializer serializer = CommonSerializer.getByCode(serializerCode);
        if (serializer == null) {
            logger.error("不识别的反序列化器: {}", serializerCode);
            throw new RpcException(RpcError.UNKNOWN_SERIALIZER);
        }
        //数据字节的长度
        int length = in.readInt();
        byte[] bytes = new byte[length];
        // 传输的对象
        // 通常是一个`RpcRequest`或`RpcClient`对象，取决于`Package Type`字段
        // 对象的序列化方式取决于`Serializer Type`字段
        in.readBytes(bytes);
        // 序列化器类型，标明这个包的数据的序列化方式
        Object obj = serializer.deserialize(bytes, packageClass);
        out.add(obj);
    }
}
```

```java
public class CommonEncoder extends MessageToByteEncoder {
    //魔数，表识一个协议包
    private static final int MAGIC_NUMBER = 0xCAFEBABE;

    private final CommonSerializer serializer;

    public CommonEncoder(CommonSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        out.writeInt(MAGIC_NUMBER);
        // 包类型，标明这是一个调用请求还是调用响应
        if (msg instanceof RpcRequest) {
            out.writeInt(PackageType.REQUEST_PACK.getCode());
        } else {
            out.writeInt(PackageType.RESPONSE_PACK.getCode());
        }
        // 序列化器类型，标明这个包的数据的序列化方式
        out.writeInt(serializer.getCode());
        byte[] bytes = serializer.serialize(msg);
        // 数据字节的长度
        out.writeInt(bytes.length);
        // 传输的对象，
        // 通常是一个`RpcRequest`或`RpcClient`对象，取决于`Package Type`字段，
        // 对象的序列化方式取决于`Serializer Type`字段
        out.writeBytes(bytes);
    }
}
```



## 使用

### Netty使用

```java
public class RpcServer extends AbstractRpcServer {
	public void start() {
        //默认起的线程数为：cpu核心数*2
        //一个线程对应一个NioEventLoop

        //bossGroup用于接受连接，workerGroup用于具体的处理
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            //启动引导、辅助类
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            //给引导类配置线程组，确认线程模型
            serverBootstrap.group(bossGroup, workerGroup)
                    //指定IO模型:
                    // (处理accept事件,与client建立连接,生成NioScocketChannel,并将其注册到某个worker NIOEventLoop上的selector)
                    .channel(NioServerSocketChannel.class)
                    //打印日志
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .option(ChannelOption.SO_BACKLOG, 256)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS))
                                    .addLast(new CommonEncoder(serializer))
                                    .addLast(new CommonDecoder())
                                    .addLast(new RpcServerHandler());
                        }
                    });
            //绑定本地端口，等待客户端的连接
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            ShutdownHook.getShutdownHook().addClearAllHook(new InetSocketAddress(host,port));
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("启动服务器时有错误发生: ", e);
        } finally {
            //关闭线程资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

### 简单的服务发布和调用过程

#### 定义调用接口

```java
/**
 * 测试用api的接口
 *
 * @author zhuwenjin
 */
public interface HelloService {
    String hello(HelloObject object);
}
```

#### 在服务提供侧实现该接口

```java
package com.zhuwenjin;

import com.zhuwenjin.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhuwenjin
 */
@Service
// @Service 放在一个类上，标识这个类提供一个服务
// @Service 注解的值定义为该服务的名称，默认值是该类的完整类名
public class HelloServiceImpl implements HelloService {
    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(HelloObject object) {
        logger.info("接收到：{}", object.getMessage());
        return "这是调用的返回值，id=" + object.getId();
    }

}

```

#### 编写服务提供者

```java
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
```

这里指定序列化方式为 Protobuf 方式。

#### 在服务消费侧远程调用

```java
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

        HelloObject object = new HelloObject(12, "This is a message");
        String res = helloService.hello(object);
        System.out.println(res);
    }
}
```

这里客户端也选用序列化方式采用 Protobuf 方式，负载均衡策略指定为轮转方式。

