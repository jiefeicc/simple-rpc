package com.zhuwenjin.netty;

import com.zhuwenjin.IRpcClient;
import com.zhuwenjin.discovery.IzkServiceDiscovery;
import com.zhuwenjin.discovery.ZkServiceDiscovery;
import com.zhuwenjin.entity.RpcRequest;
import com.zhuwenjin.entity.RpcResponse;
import com.zhuwenjin.enumeration.RpcError;
import com.zhuwenjin.exception.RpcException;
import com.zhuwenjin.loadbalancer.LoadBalancer;
import com.zhuwenjin.loadbalancer.RandomLoadBalancer;
import com.zhuwenjin.serializer.CommonSerializer;
import com.zhuwenjin.util.RpcMessageChecker;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/**
 * NIO方式消费侧客户端类
 *
 * @author zhuwenjin
 */
public class RpcClient implements IRpcClient {

    private static final Logger logger = LoggerFactory.getLogger(RpcClient.class);
    private static final Bootstrap bootstrap;
    private final IzkServiceDiscovery izkserviceDiscovery;

    private final CommonSerializer serializer;

    static {
        EventLoopGroup group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true);
    }

    public RpcClient() {
        this(DEFAULT_SERIALIZER, new RandomLoadBalancer());
    }
    public RpcClient(LoadBalancer loadBalancer) {
        this(DEFAULT_SERIALIZER, loadBalancer);
    }
    public RpcClient(Integer serializer) {
        this(serializer, new RandomLoadBalancer());
    }
    public RpcClient(Integer serializer, LoadBalancer loadBalancer) {
        this.izkserviceDiscovery = new ZkServiceDiscovery(loadBalancer);
        this.serializer = CommonSerializer.getByCode(serializer);
    }

    @Override
    public Object sendRequest(RpcRequest rpcRequest) {
        if(serializer == null) {
            logger.error("未设置序列化器");
            throw new RpcException(RpcError.SERIALIZER_NOT_FOUND);
        }
        AtomicReference<Object> result = new AtomicReference<>(null);
        try {
            InetSocketAddress inetSocketAddress = izkserviceDiscovery.lookupService(rpcRequest.getInterfaceName());
            Channel channel = ChannelProvider.get(inetSocketAddress, serializer);
            if(channel.isActive()) {
                channel.writeAndFlush(rpcRequest).addListener(future1 -> {
                    if (future1.isSuccess()) {
                        System.out.println("客户端发送消息:"+rpcRequest.toString());
                        logger.info(String.format("客户端发送消息:", rpcRequest.toString()));
                    } else {
                        logger.error("发送消息时有错误发生: ", future1.cause());
                    }
                });
                channel.closeFuture().sync();
                AttributeKey<RpcResponse> key = AttributeKey.valueOf("rpcResponse" + rpcRequest.getRequestId());
                RpcResponse rpcResponse = channel.attr(key).get();
                RpcMessageChecker.check(rpcRequest, rpcResponse);
                System.out.println("客户端收到回复:"+rpcResponse);
                result.set(rpcResponse.getData());
            } else {
                System.exit(0);
            }
        } catch (InterruptedException e) {
            logger.error("发送消息时有错误发生: ", e);
        }
        return result.get();
    }


}
