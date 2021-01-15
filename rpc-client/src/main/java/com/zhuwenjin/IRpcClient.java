package com.zhuwenjin;

import com.zhuwenjin.entity.RpcRequest;
import com.zhuwenjin.serializer.CommonSerializer;

/**
 * 客户端类通用接口
 *
 * @author ziyang
 */
public interface IRpcClient {

    int DEFAULT_SERIALIZER = CommonSerializer.KRYO_SERIALIZER;

    Object sendRequest(RpcRequest rpcRequest);

}
