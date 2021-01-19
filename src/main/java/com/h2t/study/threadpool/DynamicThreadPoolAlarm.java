package com.h2t.study.threadpool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 线程池告警
 *
 * @作者 尹吉欢
 * @个人微信 jihuan900
 * @微信公众号 猿天地
 * @GitHub https://github.com/yinjihuan
 * @作者介绍 http://cxytiandi.com/about
 * @时间 2020-05-26 21:44
 */
@Component
public class DynamicThreadPoolAlarm {

    @Autowired
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    @Autowired
    private DynamicThreadPoolProperties dynamicThreadPoolProperties;



    /**
     * 应用名称，告警用到
     */
    @Value("${spring.application.name:unknown}")
    private String applicationName;

    /**
     * 是否使用默认告警
     */
    @Value("${kitty.threadpools.alarm.default:true}")
    private boolean useDefaultAlarm;

    @PostConstruct
    public void init() {
        new Thread(() -> {
            while (true) {
                dynamicThreadPoolProperties.getExecutors().stream().forEach(prop -> {
                    String threadPoolName = prop.getThreadPoolName();
                    BqThreadPoolExecutor threadPoolExecutor = dynamicThreadPoolManager.getThreadPoolExecutor(threadPoolName);

                    dynamicThreadPoolManager.registerStatusExtension(prop, threadPoolExecutor);

                    int queueCapacityThreshold = prop.getQueueCapacityThreshold();
                    int taskCount = threadPoolExecutor.getQueue().size();
                    if (taskCount > queueCapacityThreshold) {
//                        sendQueueCapacityThresholdAlarmMessage(prop, taskCount);
                    }

                    AtomicLong rejectCount = dynamicThreadPoolManager.getRejectCount(threadPoolName);
                    if (rejectCount != null && rejectCount.get() > 0) {
//                        sendRejectAlarmMessage(rejectCount.get(), prop);
                        // 清空拒绝数据
//                        dynamicThreadPoolManager.clearRejectCount(threadPoolName);
                    }

                });
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }





}
