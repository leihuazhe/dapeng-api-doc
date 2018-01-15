package com.today.api.doc.utils;

import com.github.dapeng.client.netty.TSoaTransport;
import com.github.dapeng.core.InvocationContext;
import com.github.dapeng.core.SoaException;
import com.github.dapeng.core.metadata.Method;
import com.github.dapeng.core.metadata.Service;
import com.github.dapeng.json.JsonPost;
import com.github.dapeng.json.JsonSerializer;
import com.github.dapeng.json.SoaJsonConnectionImpl;
import com.github.dapeng.util.SoaSystemEnvProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Desc: TODO
 * author: maple
 * Date: 2018-01-15 14:48
 */
public class JsonPostUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonPostUtils.class);

    private String host = "127.0.0.1" ;

    private Integer port = 9095;

    private boolean doNotThrowError = false;

    public JsonPostUtils(String host, Integer port, boolean doNotThrowError) {
        this.host = host;
        this.port = port;
        this.doNotThrowError = doNotThrowError;
    }
    public static String callServiceMethod(InvocationContext invocationContext,
                                           String jsonParameter,
                                           Service service,
                                           String host, Integer port,Boolean doNotThrowError) throws Exception {

        if (null == jsonParameter || "".equals(jsonParameter.trim())) {
            jsonParameter = "{}" ;
        }

        List<Method> targetMethods = service.getMethods().stream().filter(_method ->
                _method.name.equals(invocationContext.getMethodName()))
                .collect(Collectors.toList());

        if (targetMethods.isEmpty()) {
            return "method:" + invocationContext.getMethodName() + " for service:"
                    + invocationContext.getServiceName() + " not found" ;
        }

        Method method = targetMethods.get(0);


        JsonSerializer jsonEncoder = new JsonSerializer(service, method, method.request, jsonParameter);
        JsonSerializer jsonDecoder = new JsonSerializer(service, method, method.response);

        final long beginTime = System.currentTimeMillis();

        LOGGER.info("soa-request: {}", jsonParameter);

        String jsonResponse = post(invocationContext.getServiceName(), invocationContext.getVersionName(),
                method.name,jsonParameter, jsonEncoder, jsonDecoder,host,port,doNotThrowError);

        LOGGER.info("soa-response: {} {}ms", jsonResponse, System.currentTimeMillis() - beginTime);

        return jsonResponse;



    }

    /**
     * 构建客户端，发送和接收请求
     *
     * @return
     */
    private static String post(String serviceName, String version, String method,
                               String requestJson, JsonSerializer jsonEncoder,
                               JsonSerializer jsonDecoder, String host, Integer port,Boolean doNotThrowError) throws Exception {

        String jsonResponse = "{}" ;

        TSoaTransport inputSoaTransport = null;
        TSoaTransport outputSoaTransport = null;

        try {
            Object result = new SoaJsonConnectionImpl(host, port).send(serviceName, version, method, requestJson, jsonEncoder, jsonDecoder);

            jsonResponse = (String) result;

        } catch (SoaException e) {

            LOGGER.error(e.getMsg());
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", e.getCode(), e.getMsg(), "{}");
            } else {
                throw e;
            }

        }  catch (Exception e) {

            LOGGER.error(e.getMessage(), e);
            if (doNotThrowError) {
                jsonResponse = String.format("{\"responseCode\":\"%s\", \"responseMsg\":\"%s\", \"success\":\"%s\"}", "9999", "系统繁忙，请稍后再试[9999]！", "{}");
            } else {
                throw e;
            }

        } finally {
            if (outputSoaTransport != null) {
                outputSoaTransport.close();
            }

            if (inputSoaTransport != null) {
                inputSoaTransport.close();
            }
        }

        return jsonResponse;
    }
}
