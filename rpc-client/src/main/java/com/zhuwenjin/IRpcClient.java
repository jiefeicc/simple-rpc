package com.zhuwenjin;

import com.zhuwenjin.entity.RpcRequest;
import com.zhuwenjin.serializer.CommonSerializer;

/**
 * 客户端类通用接口
 *
 * @author ziyang
 */
public interface IRpcClient {

    Object sendRequest(RpcRequest rpcRequest);

    void setSerializer(CommonSerializer serializer);

}
