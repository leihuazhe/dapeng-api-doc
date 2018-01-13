package com.today.api.doc;

import com.today.api.doc.properties.ApiDocProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 启动类
 *
 * @author leihuazhe
 * @date 2018-01-12 20:00
 */
@SpringBootApplication
@EnableConfigurationProperties(ApiDocProperties.class)
public class ApiDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiDocApplication.class, args);
    }
}
