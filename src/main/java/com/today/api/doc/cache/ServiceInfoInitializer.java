package com.today.api.doc.cache;

import com.today.api.doc.properties.ApiDocProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Desc: ServiceInfoInitializer 初始化
 * @author: maple
 * @Date: 2018-01-12 17:33
 */
@Component
public class ServiceInfoInitializer {

    private ZookeeperWatcher zookeeperWatcher;

    @Autowired
    private ApiDocProperties apiDocProperties;


    @PostConstruct
    public void init() {
        zookeeperWatcher = new ZookeeperWatcher(apiDocProperties.getZookeeperHost());
        zookeeperWatcher.init();
    }

    @PreDestroy
    public void destory() {
        zookeeperWatcher.destroy();
    }
}
