package com.cha.common.web.config;

import com.cha.common.core.constant.BaseConstant;
import com.cha.common.core.context.ThreadContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author songChaoHua
 * @Description 自定义异步线程池
 * @date 2023/03/25 14:40
 */

@AutoConfigureBefore(TaskExecutionAutoConfiguration.class)
@Configuration
@EnableAsync(proxyTargetClass = true)
@Slf4j
public class AsyncExecutorConfig {

    @Value("${executor.corePoolSize:10}")
    private int corePoolSize;

    @Value("${executor.maxPoolSize:10}")
    private int maxPoolSize;

    @Value("${executor.queueCapacity:1024}")
    private int queueCapacity;

    @Bean("chaExecutor")
    @ConditionalOnMissingBean(name = "chaExecutor")
    public Executor chaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        int THREAD_CORE_SIZE = Runtime.getRuntime().availableProcessors();
        log.info("CPU核数：{}C", THREAD_CORE_SIZE);
        //核心线程
        executor.setCorePoolSize(Math.max(THREAD_CORE_SIZE, corePoolSize));
        //最大线程
        executor.setMaxPoolSize(Math.max(THREAD_CORE_SIZE * BaseConstant.INT_STATUS_2, maxPoolSize));
        //队列容量
        executor.setQueueCapacity(queueCapacity);
        //保持时间
        executor.setKeepAliveSeconds(60);
        //名称前缀
        executor.setThreadNamePrefix("cha-async-");
        // CALLER_RUNS：不在新线程中执行任务，而是有调用者所在的线程来执行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setTaskDecorator(new ContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 创建线程池
     *
     * @param coreSize
     * @param maxSize
     * @param queueCapacity
     * @param threadNamePrefix
     * @param rejectedExecutionHandler
     * @return
     */
    public static ThreadPoolTaskExecutor createExecutor(int coreSize, int maxSize, int queueCapacity,
                                                        String threadNamePrefix, RejectedExecutionHandler rejectedExecutionHandler) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        //核心线程
        executor.setCorePoolSize(coreSize);
        //最大线程
        executor.setMaxPoolSize(maxSize);
        //队列容量
        executor.setQueueCapacity(queueCapacity);
        //保持时间
        executor.setKeepAliveSeconds(60);
        //名称前缀
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(rejectedExecutionHandler);
        executor.setTaskDecorator(new ContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * 创建固定线程数量的线程池
     *
     * @param size
     * @param queueCapacity
     * @param threadNamePrefix
     * @return
     */
    public static ThreadPoolTaskExecutor createExecutor(int size, int queueCapacity, String threadNamePrefix) {
        return createExecutor(size, size, queueCapacity, threadNamePrefix, new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * 创建单个线程
     *
     * @param threadNamePrefix
     * @return
     */
    public static ThreadPoolTaskExecutor createSingleExecutor(String threadNamePrefix) {
        return createExecutor(1, 1, 1, threadNamePrefix, new ThreadPoolExecutor.AbortPolicy());
    }

    public static class ContextTaskDecorator implements TaskDecorator {

        @Override
        public Runnable decorate(Runnable runnable) {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            RequestContextHolder.setRequestAttributes(requestAttributes, true);
            Map<String, Object> contextMap = ThreadContext.getContextMap();
            Map<String, String> copyOfContextMap = Optional.ofNullable(MDC.getCopyOfContextMap()).orElse(Collections.emptyMap());
            String masterThreadName = Thread.currentThread().getName();
            return () -> {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                ThreadContext.addContext(contextMap);
                MDC.setContextMap(copyOfContextMap);
                try {
                    runnable.run();
                } catch (Exception e) {
                    log.error("异步线程执行失败", e);
                    throw e;
                } finally {
                    String childThreadName = Thread.currentThread().getName();
                    if (!masterThreadName.equals(childThreadName)) {
                        //调用者线程执行的情况不清除
                        RequestContextHolder.resetRequestAttributes();
                        ThreadContext.destroy();
                        MDC.clear();
                    }
                }
            };
        }

    }

}
