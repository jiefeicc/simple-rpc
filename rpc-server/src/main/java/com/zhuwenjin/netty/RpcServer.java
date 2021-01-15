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
 * @author ziyang
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
        scanServices();
    }

    @Override
    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {

            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
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
            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            ShutdownHook.getShutdownHook().addClearAllHook(new InetSocketAddress(host,port));
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("启动服务器时有错误发生: ", e);
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
