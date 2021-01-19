package com.h2t.study.controller;

import com.h2t.study.configure.AsyncTask;
import com.h2t.study.threadpool.DynamicThreadPoolManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 控制层
 *
 * @author hetiantian
 * @version 1.0
 * @Date 2019/08/09 9:31
 */
@RestController
public class TestController {
    Logger logger = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private AsyncTask asyncTask;







    @GetMapping("/api/test")
    public Object hello() {
        long start=System.currentTimeMillis();
//        for(int i=0;i<1000000;i++){
            logger.info("【TestController.class】info level log input");

            logger.error("【TestController.class】error level log input");
//        }
        long end =System.currentTimeMillis();
       System.out.println("100w行日志总耗时:"+(end-start));
        return "hello world";
    }

    @GetMapping("/api/thread")
    public Object thread() {
        for(int i=0;i<10000;i++) {
            asyncTask.doTask1();
            asyncTask.doTask2();
        }
      return null;
    }
}
