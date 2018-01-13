package com.today.api.doc.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "api.doc")
public class ApiDocProperties {

    private String zookeeperHost = ApiDocConstants.SOA_ZOOKEEPER_HOST;

    private String parentPath = ApiDocConstants.PARENT_PATH;

    public String getZookeeperHost() {
        return zookeeperHost;
    }

    public void setZookeeperHost(String zookeeperHost) {
        this.zookeeperHost = zookeeperHost;
    }
}


