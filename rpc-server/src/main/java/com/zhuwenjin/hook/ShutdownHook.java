package com.zhuwenjin.hook;

import com.zhuwenjin.factory.ThreadPoolFactory;
import com.zhuwenjin.util.CuratorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

public class ShutdownHook {

    private static final Logger logger = LoggerFactory.getLogger(ShutdownHook.class);

    private final ExecutorService threadPool = ThreadPoolFactory.createDefaultThreadPool("shutdown-hook");
    private static final ShutdownHook shutdownHook = new ShutdownHook();

    public static ShutdownHook getShutdownHook() {
        return shutdownHook;
    }

    public void addClearAllHook(InetSocketAddress inetSocketAddress) {
        logger.info("关闭后将自动注销所有服务");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CuratorUtils.clearRegistry(inetSocketAddress);
            threadPool.shutdown();
        }));
    }

}

