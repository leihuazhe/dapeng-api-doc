package com.today.api.doc.cache;

import com.today.api.doc.properties.ApiDocProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;

/**
 * Desc: ServiceInfoInitializer 初始化
 * @author: maple
 * @Date: 2018-01-12 17:33
 */
@Component
public class ServiceInfoInitializer {

    private ZookeeperWatcher zookeeperWatcher;

//    private CuratorWatcher curatorWatcher;

    @Autowired
    private ApiDocProperties apiDocProperties;


    @PostConstruct
    public void init() {
        zookeeperWatcher = new ZookeeperWatcher(apiDocProperties.getZookeeperHost());
        zookeeperWatcher.init();

//        curatorWatcher = new CuratorWatcher(apiDocProperties.getZookeeperHost(),5000);
//        curatorWatcher.init();
    }

    /*@PreDestroy
    public void destory() {
        zookeeperWatcher.destroy();
    }*/
}
