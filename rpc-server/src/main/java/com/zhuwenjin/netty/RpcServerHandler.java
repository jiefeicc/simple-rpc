package com.zhuwenjin.netty;

import com.zhuwenjin.RequestHandler;
import com.zhuwenjin.entity.RpcRequest;
import com.zhuwenjin.entity.RpcResponse;
import com.zhuwenjin.registry.DefaultServiceRegistry;
import com.zhuwenjin.registry.IServiceRegistry;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty中处理RpcRequest的Handler
 *
 * @author zhuwenjin
 */
public class RpcServerHandler extends SimpleChannelInboundHandler<RpcRequest> {

    private static final Logger logger = LoggerFactory.getLogger(RpcServerHandler.class);
    private static RequestHandler requestHandler;
    private static IServiceRegistry serviceRegistry;

    static {
        requestHandler = new RequestHandler();
        serviceRegistry = new DefaultServiceRegistry();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcRequest msg) throws Exception {
        try {
            System.out.println("服务端接收到请求: "+msg);
            Object result = requestHandler.handle(msg);
            System.out.println("服务端接发送回复: "+RpcResponse.success(result, msg.getRequestId()));
            ChannelFuture future = ctx.writeAndFlush(RpcResponse.success(result, msg.getRequestId()));
            future.addListener(ChannelFutureListener.CLOSE);
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("处理过程调用时有错误发生:");
        cause.printStackTrace();
        ctx.close();
    }

}
