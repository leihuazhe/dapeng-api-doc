package com.today.api.doc;

import com.today.api.doc.properties.ApiDocProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.nio.charset.Charset;
import java.util.*;

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


    @Bean
    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
        MappingJackson2HttpMessageConverter mappingConverter = new MappingJackson2HttpMessageConverter();
        List<MediaType> list = new ArrayList();
        list.add(MediaType.TEXT_HTML);
        list.add(MediaType.TEXT_PLAIN);
        list.add(MediaType.APPLICATION_JSON_UTF8);
        mappingConverter.setSupportedMediaTypes(list);
        return mappingConverter;
    }

}
