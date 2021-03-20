package com.zhuwenjin;

import com.zhuwenjin.annotation.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhuwenjin
 */
@Service
// @Service 放在一个类上，标识这个类提供一个服务
// @Service 注解的值定义为该服务的名称，默认值是该类的完整类名
public class HelloServiceImpl implements HelloService {

    private static final Logger logger = LoggerFactory.getLogger(HelloServiceImpl.class);

    @Override
    public String hello(HelloObject object) {
        logger.info("接收到：{}", object.getMessage());
        return "这是调用的返回值，id=" + object.getId();
    }

}
