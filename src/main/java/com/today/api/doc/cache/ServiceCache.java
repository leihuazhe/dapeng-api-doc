package com.today.api.doc.cache;


import com.github.dapeng.client.netty.SoaConnectionImpl;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.*;
import com.github.dapeng.metadata.*;
import com.github.dapeng.registry.ServiceInfo;
import com.github.dapeng.util.SoaSystemEnvProperties;
import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Service Cache
 *
 * @author maple
 * @date 2018/1/12 17:26
 */
@SuppressWarnings("AlibabaUndefineMagicConstant")
public class ServiceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCache.class);

    private static Map<String, Service> services = new TreeMap<>();

    private static Map<String, Service> fullNameService = new TreeMap<>();

    public static TreeMultimap<String, String> urlMappings = TreeMultimap.create();

    public static void resetCache() {
        services.clear();
        fullNameService.clear();
        urlMappings.clear();
    }

    public static void loadServicesMetadata(String serviceName, List<ServiceInfo> infos) {

        Map<String, ServiceInfo> diffVersionServices = new HashMap<>();
        for (ServiceInfo info : infos) {
            diffVersionServices.put(info.getVersionName(), info);
        }

        for (ServiceInfo info : diffVersionServices.values()) {
            String version = info.getVersionName();
            String metadata = "";
            try {
//                soa.container.ip          soa.container.port

                metadata = new MetadataClient(serviceName, version){
                    @Override
                    public String getServiceMetadata() throws Exception {
                        getServiceMetadata_result result = new SoaConnectionImpl(info.getHost(), info.getPort())
                                .send(serviceName,version,"getServiceMetadata", new getServiceMetadata_args(),
                                        new GetServiceMetadata_argsSerializer(),new GetServiceMetadata_resultSerializer());

                        return result.getSuccess();
                    }
                }.getServiceMetadata();
                if (metadata != null) {
                    try (StringReader reader = new StringReader(metadata)) {
                        Service s = JAXB.unmarshal(reader, Service.class);
                        String serviceKey = getKey(s);
                        String fullNameKey = getFullNameKey(s);

                        services.put(serviceKey, s);
                        LOGGER.info("----------------- service size :  "+services.size());
                        fullNameService.put(fullNameKey, s);
                        loadServiceUrl(s);

                    } catch (Exception e) {
                        LOGGER.error("{}:{} metadata解析出错", serviceName, version);
                        LOGGER.debug(metadata);
                    }
                }
            } catch (SoaException e) {
                LOGGER.error("{}:{} metadata获取出错", serviceName, version);
                LOGGER.error("metadata获取出错", e);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * 将service和service中的方法、结构体、枚举和字段名分别设置对应的url，以方便搜索
     * @param service
     */
    private static void loadServiceUrl(Service service) {
        //将service和service中的方法、结构体、枚举和字段名分别设置对应的url，以方便搜索
        urlMappings.put(service.getName(), "api/service/" + service.name + "/" + service.meta.version + ".htm");
        List<Method> methods = service.getMethods();
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            urlMappings.put(method.name, "api/method/" + service.name + "/" + service.meta.version + "/" + method.name + ".htm");
        }

        List<Struct> structs = service.getStructDefinitions();
        for (int i = 0; i < structs.size(); i++) {
            Struct struct = structs.get(i);
            urlMappings.put(struct.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");

            List<Field> fields = struct.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                urlMappings.put(field.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");
            }
        }

        List<TEnum> tEnums = service.getEnumDefinitions();
        for (int i = 0; i < tEnums.size(); i++) {
            TEnum tEnum = tEnums.get(i);
            urlMappings.put(tEnum.name, "api/enum/" + service.name + "/" + service.meta.version + "/" + tEnum.namespace + "." + tEnum.name + ".htm");
        }


    }




    public void destory() {
        services.clear();
    }



    public static Service getService(String name, String version) {

        if (name.contains(".")) {
            return fullNameService.get(getKey(name, version));
        } else {
            return services.get(getKey(name, version));
        }
    }

    private static String getKey(Service service) {
        return getKey(service.getName(), service.getMeta().version);
    }

    private static String getFullNameKey(Service service) {
        return getKey(service.getNamespace() + "." + service.getName(), service.getMeta().version);
    }

    private static String getKey(String name, String version) {
        return name + ":" + version;
    }

    public static Map<String, Service> getServices() {
        return services;
    }

}
