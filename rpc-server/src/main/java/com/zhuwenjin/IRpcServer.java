package com.zhuwenjin;

import com.zhuwenjin.serializer.CommonSerializer;

/**
 * 服务器类通用接口
 *
 * @author ziyang
 */
public interface IRpcServer {

    void start();

    void start(int port);

    void setSerializer(CommonSerializer serializer);

    <T> void publishService(T service, Class<T> serviceClass);

}
