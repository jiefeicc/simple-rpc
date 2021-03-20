package com.zhuwenjin.netty;

import com.zhuwenjin.AbstractRpcServer;
import com.zhuwenjin.codec.CommonDecoder;
import com.zhuwenjin.codec.CommonEncoder;
import com.zhuwenjin.hook.ShutdownHook;
import com.zhuwenjin.provider.ServiceProviderImpl;
import com.zhuwenjin.registry.ZkServiceRegistry;
import com.zhuwenjin.serializer.CommonSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;


import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;


/**
 * NIO方式服务提供侧
 *
 * @author zhuwenjin
 */
public class RpcServer extends AbstractRpcServer {
    private final CommonSerializer serializer;

    public RpcServer(String host, int port) {
        this(host, port, DEFAULT_SERIALIZER);
    }

    public RpcServer(String host, int port, Integer serializer) {
        this.host = host;
        this.port = port;
        izkserviceRegistry = new ZkServiceRegistry();
        serviceProvider = new ServiceProviderImpl();
        this.serializer = CommonSerializer.getByCode(serializer);
        //扫描服务并向zookeeper注册服务
        scanServices();
    }

    @Override
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
