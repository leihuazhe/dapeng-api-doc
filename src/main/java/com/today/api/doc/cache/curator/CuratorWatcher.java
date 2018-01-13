package com.today.api.doc.cache.curator;

import com.github.dapeng.registry.ServiceInfo;
import com.today.api.doc.properties.ApiDocConstants;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CuratorWatcher
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
        //1 重试策略：初试时间为1s 重试10次
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(1000, 10);
        //2 通过工厂创建连接
        cf = CuratorFrameworkFactory.builder()
                .connectString(zookeeperHost)
                .sessionTimeoutMs(sessionTimeOut)
                .retryPolicy(retryPolicy)
                .build();

        cf.getConnectionStateListenable().addListener((client, state) -> {

            if (state == ConnectionState.LOST) {
                while (true) {
                    boolean flag = false;
                    try {
                        flag = cf.getZookeeperClient().blockUntilConnectedOrTimedOut();
                        if (flag) {
                            //重新获取节点
//                    clearListener();
//                    createNode(nodePath, nodeData);
//                    client.getConnectionStateListenable().addListener(getListener(nodePath, nodeData));
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }


//                CuratorWatcher.this.stateChanged(StateListener.DISCONNECTED);
                //连接丢失
                LOGGER.info("lost session with zookeeper");
            } else if (state == ConnectionState.CONNECTED) {
//                CuratorWatcher.this.stateChanged(StateListener.CONNECTED);
                //连接新建
                LOGGER.info("connected with zookeeper");
            } else if (state == ConnectionState.RECONNECTED) {
//                CuratorWatcher.this.stateChanged(StateListener.RECONNECTED);
                LOGGER.info("reconnected with zookeeper");
            }
        });


        //3 建立连接
        cf.start();


    }

    public void dos() throws Exception {
        //4 创建跟节点
        if (cf.checkExists().forPath(PARENT_PATH) == null) {
            cf.create().withMode(CreateMode.PERSISTENT).forPath(PARENT_PATH, "super init".getBytes());
        }
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
                        System.out.println("CHILD_ADDED :" + new String(event.getData().getData()));
                        break;
                    case CHILD_UPDATED:
                        System.out.println("CHILD_UPDATED :" + event.getData().getPath());
                        System.out.println("CHILD_UPDATED :" + new String(event.getData().getData()));
                        break;
                    case CHILD_REMOVED:
                        System.out.println("CHILD_REMOVED :" + event.getData().getPath());
                        System.out.println("CHILD_REMOVED :" + new String(event.getData().getData()));
                        break;
                    default:
                        break;
                }
            }
        });
    }
}
