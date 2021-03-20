package com.zhuwenjin.codec;

import com.zhuwenjin.entity.RpcRequest;
import com.zhuwenjin.enumeration.PackageType;
import com.zhuwenjin.serializer.CommonSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;


/**
 * 通用的编码拦截器
 */
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
