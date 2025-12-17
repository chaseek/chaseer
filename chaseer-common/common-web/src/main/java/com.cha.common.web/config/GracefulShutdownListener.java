package com.cha.common.web.config;

import com.cha.common.core.utils.ThreadUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;

/**
 * @author ztc
 * @since 2025-04-01
 */
@Slf4j
@Component
public class GracefulShutdownListener implements ApplicationListener<ContextClosedEvent>, ApplicationRunner {

    @Autowired
    private ServiceRegistry<Registration> serviceRegistry;

    @Autowired
    private Registration registration;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        // 通知 Nacos，开始下线
        serviceRegistry.setStatus(registration, "DOWN");
        ThreadUtil.sleep(6000);
        serviceRegistry.deregister(registration);
        System.out.println("服务下线" + JSON.toJSONString(registration));
        log.info("服务下线：{}", JSON.toJSONString(registration));
    }

    @Override
    public void run(ApplicationArguments args) {
        // 通知 Nacos，上线
        serviceRegistry.register(registration);
        serviceRegistry.setStatus(registration, "UP");
        log.info("服务上线，name={},ip={},port={}", registration.getServiceId(), registration.getHost(), registration.getPort());
    }

}
