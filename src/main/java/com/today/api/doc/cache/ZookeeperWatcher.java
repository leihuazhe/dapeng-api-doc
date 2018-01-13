package com.today.api.doc.cache;

import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.registry.zookeeper.WatcherUtils;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tangliu on 2016/2/29.
 */
public class ZookeeperWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperWatcher.class);
    private final static Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();
    private String zookeeperHost;

    public ZookeeperWatcher(String zookeeperHost) {
        this.zookeeperHost = zookeeperHost;
    }

    private ZooKeeper zk;

    public void init() {
        connect();
        getServersList();
    }

    public void destroy() {
        if (zk != null) {
            try {
                zk.close();
                zk = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        caches.clear();
        LOGGER.info("关闭连接，清空service info caches");
    }

    public static Map<String, List<ServiceInfo>> getServices() {
        return caches;
    }


    private final static String serviceRoute = "/soa/runtime/services";

    /**
     * 获取zookeeper中的services节点的子节点，并设置监听器
     *
     * @return
     */
    public void getServersList() {

        ServiceCache.resetCache();
        caches.clear();

        try {
            List<String> children = zk.getChildren(serviceRoute, watchedEvent -> {
                //Children发生变化，则重新获取最新的services列表
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取子节点...", watchedEvent.getPath());
                    getServersList();
                }
            });

            children.forEach(serviceName -> getServiceInfoByServiceName(serviceName));
        } catch (KeeperException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 根据serviceName节点的路径，获取下面的子节点，并监听子节点变化
     *
     * @param serviceName
     */
    private void getServiceInfoByServiceName(String serviceName) {

        String servicePath = serviceRoute + "/" + serviceName;
        try {

            if (zk == null){
                init();
            }

            List<String> children = zk.getChildren(servicePath, watchedEvent -> {
                if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                    LOGGER.info("{}子节点发生变化，重新获取信息", watchedEvent.getPath());
                    getServiceInfoByServiceName(serviceName);
                }
            });

            LOGGER.info("获取{}的子节点成功", servicePath);
            WatcherUtils.resetServiceInfoByName(serviceName, servicePath, children, caches);

            ServiceCache.loadServicesMetadata(serviceName, caches.get(serviceName));

        } catch (KeeperException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * 连接zookeeper
     */
    private void connect() {
        try {

            zk = new ZooKeeper(zookeeperHost, 15000, e -> {
                if (e.getState() == Watcher.Event.KeeperState.Expired) {
                    LOGGER.info("zookeeper Watcher 到zookeeper Server的session过期，重连");
                    destroy();
                    init();
                } else if (e.getState() == Watcher.Event.KeeperState.SyncConnected) {
                    LOGGER.info("Zookeeper Watcher 已连接 zookeeper Server,Zookeeper host: {}",zookeeperHost);
                }else if (e.getState() == Watcher.Event.KeeperState.Disconnected) {
                    LOGGER.info("Zookeeper Watcher 连接不上了");
                }
            });
        } catch (Exception e) {
            LOGGER.info(e.getMessage(), e);
        }
    }
}
