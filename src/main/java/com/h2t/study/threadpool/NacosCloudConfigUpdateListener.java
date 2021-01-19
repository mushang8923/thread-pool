package com.h2t.study.threadpool;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Spring Cloud Alibaba Nacos配置修改监听
 *
 * @作者 尹吉欢
 * @个人微信 jihuan900
 * @微信公众号 猿天地
 * @GitHub https://github.com/yinjihuan
 * @作者介绍 http://cxytiandi.com/about
 * @时间 2020-06-18 23:06
 */
@Slf4j
@Component
public class NacosCloudConfigUpdateListener {

    @Autowired
    private NacosConfigProperties nacosConfigProperties;

    @Autowired
    private DynamicThreadPoolManager dynamicThreadPoolManager;

    @Autowired
    private DynamicThreadPoolProperties poolProperties;


    @PostConstruct
    public void init() {
        initConfigUpdateListener();
    }

    public void initConfigUpdateListener() {
        ConfigService configService = nacosConfigProperties.configServiceInstance();
        Assert.hasText(poolProperties.getNacosDataId(), "请配置bq.threadpools.nacosDataId");
        Assert.hasText(poolProperties.getNacosGroup(), "请配置bq.threadpools.nacosGroup");

        try {
            configService.addListener(poolProperties.getNacosDataId(), poolProperties.getNacosGroup(), new AbstractListener() {
                @SneakyThrows
                @Override
                public void receiveConfigInfo(String configInfo) {
                    List<ThreadPoolProperties> exector=JSONArray.parseArray(configInfo,ThreadPoolProperties.class);
                    poolProperties.setExecutors(exector);
                    new Thread(() -> dynamicThreadPoolManager.refreshThreadPoolExecutor(true)).start();
                    log.info("线程池配置有变化，刷新完成");
                }
            });
        } catch (NacosException e) {
            log.error("Nacos配置监听异常", e);
        }
    }

}
