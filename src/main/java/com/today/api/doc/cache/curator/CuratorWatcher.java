package com.today.api.doc.cache.curator;

import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.registry.zookeeper.WatcherUtils;
import com.today.api.doc.cache.ServiceCache;
import com.today.api.doc.properties.ApiDocConstants;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CuratorWatcher 使用Curator api 实现
 *
 * @author maple
 * @date 2018/1/13 17:26
 */
public class CuratorWatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CuratorWatcher.class);
    private final static Map<String, List<ServiceInfo>> caches = new ConcurrentHashMap<>();
    private String zookeeperHost;
    private Integer sessionTimeOut;
    private String PARENT_PATH = ApiDocConstants.PARENT_PATH;

    public String getZookeeperHost() {
        return zookeeperHost;
    }

    public void setZookeeperHost(String zookeeperHost) {
        this.zookeeperHost = zookeeperHost;
    }

    public Integer getSessionTimeOut() {
        return sessionTimeOut;
    }

    public void setSessionTimeOut(Integer sessionTimeOut) {
        this.sessionTimeOut = sessionTimeOut;
    }

    private CuratorFramework cf;


    public void init() {
        //1 通过工厂创建连接
        cf = CuratorFrameworkFactory.builder()
                .connectString(zookeeperHost)
                .sessionTimeoutMs(sessionTimeOut)
                .retryPolicy(new RetryNTimes(1, 1000))
                .build();

        cf.getConnectionStateListenable().addListener((client, state) -> {
            if (state == ConnectionState.LOST) {
                //连接丢失
                LOGGER.info("lost session with zookeeper");
                while (true) {
                    boolean flag;
                    try {
                        flag = cf.getZookeeperClient().blockUntilConnectedOrTimedOut();
                        if (flag) {
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            } else if (state == ConnectionState.CONNECTED) {
                //连接新建
                LOGGER.info("connected with zookeeper");
            } else if (state == ConnectionState.RECONNECTED) {
                LOGGER.info("reconnected with zookeeper");
            }
        });
        //3 建立连接
        cf.start();
    }

    public void getServerList() throws Exception {
        //4 建立一个PathChildrenCache缓存,第三个参数为是否接受节点数据内容 如果为false则不接受
        PathChildrenCache cache = new PathChildrenCache(cf, PARENT_PATH, true);
        //5 在初始化的时候就进行缓存监听
        cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
        cache.getListenable().addListener(new PathChildrenCacheListener() {
            /**
             * <B>方法名称：</B>监听子节点变更<BR>
             * <B>概要说明：</B>新建、修改、删除<BR>
             * @see PathChildrenCacheListener#childEvent(CuratorFramework, PathChildrenCacheEvent)
             */
            @Override
            public void childEvent(CuratorFramework cf, PathChildrenCacheEvent event) throws Exception {
                switch (event.getType()) {
                    case CHILD_ADDED:
                        System.out.println("CHILD_ADDED :" + event.getData().getPath());
                        getServerList();
                        break;
                    case CHILD_UPDATED:
                        System.out.println("CHILD_UPDATED :" + event.getData().getPath());
                        getServerList();
                        break;
                    case CHILD_REMOVED:
                        System.out.println("CHILD_REMOVED :" + event.getData().getPath());
                        getServerList();
                        break;
                    default:
                        break;
                }
            }
        });

        List<String> children = cf.getChildren().forPath(PARENT_PATH);

        children.forEach(serviceName -> getServiceInfoByServiceName(serviceName));

    }

    public void getServiceInfoByServiceName(String serviceName) {

        String servicePath = PARENT_PATH + "/" + serviceName;
        try {
            PathChildrenCache cache = new PathChildrenCache(cf, servicePath, true);
            //5 在初始化的时候就进行缓存监听
            cache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
            cache.getListenable().addListener((cf, event) -> {
                switch (event.getType()) {
                    case CHILD_ADDED:
                        System.out.println("CHILD_ADDED :" + event.getData().getPath());
                        getServiceInfoByServiceName(serviceName);
                        break;
                    case CHILD_UPDATED:
                        System.out.println("CHILD_UPDATED :" + event.getData().getPath());
                        break;
                    case CHILD_REMOVED:
                        System.out.println("CHILD_REMOVED :" + event.getData().getPath());
                        getServiceInfoByServiceName(serviceName);
                        break;
                    default:
                        break;
                }
            });

            List<String> children = cf.getChildren().forPath(servicePath);

            LOGGER.info("获取{}的子节点成功", servicePath);
            WatcherUtils.resetServiceInfoByName(serviceName, servicePath, children, caches);

            ServiceCache.loadServicesMetadata(serviceName, caches.get(serviceName));

        } catch (KeeperException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
