package com.h2t.study.threadpool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadPoolExecutor;


/**
 * 将线程池对象交由spring托管
 */
@Component
public class ThreadPoolBeanConfig implements ApplicationRunner {

    @Autowired
    private ConfigurableListableBeanFactory configurableListableBeanFactory;

    @Autowired
    private DynamicThreadPoolProperties dynamicThreadPoolProperties;

    @Autowired
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        dynamicThreadPoolProperties.getExecutors().forEach(executor -> {
            ThreadPoolExecutor threadPoolExecutor = dynamicThreadPoolManager.getThreadPoolExecutorMap().get(executor.getThreadPoolName());
            configurableListableBeanFactory.registerSingleton(executor.getThreadPoolName(),threadPoolExecutor);
        });
    }
}
