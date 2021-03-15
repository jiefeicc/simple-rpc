package com.zhuwenjin.util;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 操作zookeeper的客户端
 */
public final class CuratorUtils {
    private static final Logger logger = LoggerFactory.getLogger(CuratorUtils.class);

    //baseSleepTimeMs：重试之间等待的初始时间 maxRetries ：最大重试次数
    private static final int BASE_SLEEP_TIME = 1000;
    private static final int MAX_RETRIES = 3;
    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "120.53.121.68:2181";
    private static CuratorFramework zkClient;
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    private CuratorUtils() { }

    public static void createPersistentNode(String path) {
        zkClient=getZkClient();
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                logger.info("The node already exists. The node is:[{}]", path);
            } else {
                //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                logger.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            logger.error("create persistent node for path [{}] fail", path);
        }
    }

    public static List<String> getChildrenNodes(String rpcServiceName) {
        zkClient=getZkClient();
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            logger.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    public static void clearRegistry(InetSocketAddress inetSocketAddress) {
        zkClient=getZkClient();
        try {
            zkClient.delete().deletingChildrenIfNeeded().forPath(ZK_REGISTER_ROOT_PATH);
            System.out.println("Server端关闭时，注销所有服务");
        } catch (Exception e) {
            logger.error("clear registry for path [{}] fail", ZK_REGISTER_ROOT_PATH);
        }
        logger.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
    }

    public static CuratorFramework getZkClient() {
        // if zkClient has been started, return directly
        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }
        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder()
                // the server to connect to (can be a server list)
                .connectString(DEFAULT_ZOOKEEPER_ADDRESS)
                .retryPolicy(retryPolicy)
                .build();
        zkClient.start();
        return zkClient;
    }

        private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        };
        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

}
//
//    private static final int BASE_SLEEP_TIME = 1000;
//    private static final int MAX_RETRIES = 3;
//    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";
//    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
//    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();
//    private static CuratorFramework zkClient;
//    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "120.53.121.68:2181";
//
//    private CuratorUtils() {
//    }
//
//    /**
//     * Create persistent nodes. Unlike temporary nodes, persistent nodes are not removed when the client disconnects
//     *
//     * @param path node path
//     */
//    public static void createPersistentNode(CuratorFramework zkClient, String path) {
//        try {
//            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
//                log.info("The node already exists. The node is:[{}]", path);
//            } else {
//                //eg: /my-rpc/github.javaguide.HelloService/127.0.0.1:9999
//                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
//                log.info("The node was created successfully. The node is:[{}]", path);
//            }
//            REGISTERED_PATH_SET.add(path);
//        } catch (Exception e) {
//            log.error("create persistent node for path [{}] fail", path);
//        }
//    }
//
//    /**
//     * Gets the children under a node
//     *
//     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
//     * @return All child nodes under the specified node
//     */
//    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
//        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
//            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
//        }
//        List<String> result = null;
//        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
//        try {
//            result = zkClient.getChildren().forPath(servicePath);
//            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
//            registerWatcher(rpcServiceName, zkClient);
//        } catch (Exception e) {
//            log.error("get children nodes for path [{}] fail", servicePath);
//        }
//        return result;
//    }
//
//    /**
//     * Empty the registry of data
//     */
//    public static void clearRegistry(InetSocketAddress inetSocketAddress) {
//        CuratorFramework zkClient = CuratorUtils.getZkClient();
//        try {
//            zkClient.delete().deletingChildrenIfNeeded().forPath(ZK_REGISTER_ROOT_PATH);
//            System.out.println("Server端关闭时，注销所有服务");
//        } catch (Exception e) {
//            log.error("clear registry for path [{}] fail", ZK_REGISTER_ROOT_PATH);
//        }
//        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET.toString());
//    }
//
//    public static CuratorFramework getZkClient() {
//        // check if user has set zk address
//        Properties properties = PropertiesFileUtil.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
//        String zookeeperAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ? properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;
//        // if zkClient has been started, return directly
//        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
//            return zkClient;
//        }
//        // Retry strategy. Retry 3 times, and will increase the sleep time between retries.
//        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
//        zkClient = CuratorFrameworkFactory.builder()
//                // the server to connect to (can be a server list)
//                .connectString(zookeeperAddress)
//                .retryPolicy(retryPolicy)
//                .build();
//        zkClient.start();
//        return zkClient;
//    }
//
//    /**
//     * Registers to listen for changes to the specified node
//     *
//     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version
//     */
//    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
//        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
//        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
//        PathChildrenCacheListener pathChildrenCacheListener = (curatorFramework, pathChildrenCacheEvent) -> {
//            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
//            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
//        };
//        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
//        pathChildrenCache.start();
//    }
//
//}
//
