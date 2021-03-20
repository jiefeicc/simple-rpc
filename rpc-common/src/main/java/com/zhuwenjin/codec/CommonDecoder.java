package com.zhuwenjin.codec;

import com.zhuwenjin.entity.RpcRequest;
import com.zhuwenjin.entity.RpcResponse;
import com.zhuwenjin.enumeration.PackageType;
import com.zhuwenjin.enumeration.RpcError;
import com.zhuwenjin.exception.RpcException;
import com.zhuwenjin.serializer.CommonSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

/**
 * 通用的解码拦截器
 *
 * @author zhuwenjin
 */
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
