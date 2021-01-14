package com.zhuwenjin.netty;

import com.zhuwenjin.IRpcServer;
import com.zhuwenjin.codec.CommonDecoder;
import com.zhuwenjin.codec.CommonEncoder;
import com.zhuwenjin.enumeration.RpcError;
import com.zhuwenjin.exception.RpcException;
import com.zhuwenjin.hook.ShutdownHook;
import com.zhuwenjin.provider.ServiceProvider;
import com.zhuwenjin.provider.ServiceProviderImpl;
import com.zhuwenjin.registry.IzkServiceRegistry;
import com.zhuwenjin.registry.ZkServiceRegistry;
import com.zhuwenjin.serializer.CommonSerializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;


/**
 * NIO方式服务提供侧
 *
 * @author ziyang
 */
public class RpcServer implements IRpcServer {
    private static final Logger logger = LoggerFactory.getLogger(RpcServer.class);

    private final String host;
    private final int port;

    private final IzkServiceRegistry izkserviceRegistry;
    private final ServiceProvider serviceProvider;

    private CommonSerializer serializer;

    public RpcServer(String host, int port) {
        this.host = host;
        this.port = port;
        izkserviceRegistry = new ZkServiceRegistry();
        serviceProvider = new ServiceProviderImpl();
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
                            pipeline.addLast(new CommonEncoder(serializer));
                            pipeline.addLast(new CommonDecoder());
                            pipeline.addLast(new RpcServerHandler());
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

    @Override
    public void setSerializer(CommonSerializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public <T> void publishService(T service, Class<T> serviceClass) {
        if(serializer == null) {
            logger.error("未设置序列化器");
            throw new RpcException(RpcError.SERIALIZER_NOT_FOUND);
        }
        serviceProvider.addServiceProvider(service, serviceClass);
        izkserviceRegistry.registerService(serviceClass.getCanonicalName(), new InetSocketAddress(host, port));
        start();
    }

}
