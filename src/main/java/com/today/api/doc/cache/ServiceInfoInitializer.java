package com.today.api.doc.cache;

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


    @PostConstruct
    public void init() {
        zookeeperWatcher = new ZookeeperWatcher();
        zookeeperWatcher.init();
    }

    @PreDestroy
    public void destory() {
        zookeeperWatcher.destroy();
    }
}
