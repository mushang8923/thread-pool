package com.h2t.study.threadpool;

import com.alibaba.cloud.nacos.NacosConfigProperties;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 动态线程池
 *
 * @作者 尹吉欢
 * @个人微信 jihuan900
 * @微信公众号 猿天地
 * @GitHub https://github.com/yinjihuan
 * @作者介绍 http://cxytiandi.com/about
 * @时间 2020-05-24 20:31
 */
@Slf4j
@Component
public class DynamicThreadPoolManager  {

    @Autowired
    private DynamicThreadPoolProperties dynamicThreadPoolProperties;

    @Autowired
    private NacosConfigProperties nacosConfigProperties;

    @Autowired
    private DynamicThreadPoolProperties poolProperties;


    /**
     * 存储线程池对象，Key:名称 Value:对象
     */
    private static Map<String, BqThreadPoolExecutor> threadPoolExecutorMap = new HashMap<>();

    /**
     * 存储线程池拒绝次数，Key:名称 Value:次数
     */
    private static Map<String, AtomicLong> threadPoolExecutorRejectCountMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        createThreadPoolExecutor(dynamicThreadPoolProperties);
    }

    /**
     * 创建线程池
     * @param threadPoolProperties
     */
    public void createThreadPoolExecutor(DynamicThreadPoolProperties threadPoolProperties) {
        ConfigService configService = nacosConfigProperties.configServiceInstance();
        String content = null;
        try {
            content = configService.getConfig(poolProperties.getNacosDataId(), poolProperties.getNacosGroup(),3000L);
        } catch (NacosException e) {
           log.error("获取配置异常");
        }

        List<ThreadPoolProperties> exector= JSONArray.parseArray(content,ThreadPoolProperties.class);
        poolProperties.setExecutors(exector);
        exector.forEach(executor -> {
            if (!threadPoolExecutorMap.containsKey(executor.getThreadPoolName())) {
                BqThreadPoolExecutor threadPoolExecutor = new BqThreadPoolExecutor(
                        executor.getCorePoolSize(),
                        executor.getMaximumPoolSize(),
                        executor.getKeepAliveTime(),
                        executor.getUnit(),
                        getBlockingQueue(executor.getQueueType(), executor.getQueueCapacity(), executor.isFair()),
                        new BqThreadFactory(executor.getThreadPoolName()),
                        getRejectedExecutionHandler(executor.getRejectedExecutionType(), executor.getThreadPoolName()), executor.getThreadPoolName());

                threadPoolExecutorMap.put(executor.getThreadPoolName(), threadPoolExecutor);
            }
        });
    }

    public void registerStatusExtension(ThreadPoolProperties prop, BqThreadPoolExecutor executor) {
                AtomicLong rejectCount = getRejectCount(prop.getThreadPoolName());

                Map<String, String> pool = new HashMap<>();

                pool.put("activeCount", String.valueOf(executor.getActiveCount()));
                pool.put("completedTaskCount", String.valueOf(executor.getCompletedTaskCount()));
                pool.put("largestPoolSize", String.valueOf(executor.getLargestPoolSize()));
                pool.put("taskCount", String.valueOf(executor.getTaskCount()));
                pool.put("rejectCount", String.valueOf(rejectCount == null ? 0 : rejectCount.get()));
                pool.put("waitTaskCount", String.valueOf(executor.getQueue().size()));

        log.info("pool {} coreSize:{},maxSize:{},blocktaskCount:{},remainingCapacity:{},activeCount:{},,completedTaskCount:{},largestPoolSize:{},taskCount:{},rejectCount:{},waitTaskCount:{}",
                prop.getThreadPoolName(),executor.getCorePoolSize(),executor.getMaximumPoolSize(),executor.getQueue().size(),executor.getQueue().remainingCapacity(),executor.getActiveCount(),executor.getCompletedTaskCount(),executor.getLargestPoolSize(),
                executor.getTaskCount(),rejectCount == null ? 0 : rejectCount.get(),executor.getQueue().size());

    }

    /**
     * 获取拒绝策略
     * @param rejectedExecutionType
     * @param threadPoolName
     * @return
     */
    private RejectedExecutionHandler getRejectedExecutionHandler(String rejectedExecutionType, String threadPoolName) {
        if (RejectedExecutionHandlerEnum.CALLER_RUNS_POLICY.getType().equals(rejectedExecutionType)) {
            return new ThreadPoolExecutor.CallerRunsPolicy();
        }
        if (RejectedExecutionHandlerEnum.DISCARD_OLDEST_POLICY.getType().equals(rejectedExecutionType)) {
            return new ThreadPoolExecutor.DiscardOldestPolicy();
        }
        if (RejectedExecutionHandlerEnum.DISCARD_POLICY.getType().equals(rejectedExecutionType)) {
            return new ThreadPoolExecutor.DiscardPolicy();
        }
        ServiceLoader<RejectedExecutionHandler> serviceLoader = ServiceLoader.load(RejectedExecutionHandler.class);
        Iterator<RejectedExecutionHandler> iterator = serviceLoader.iterator();
        while (iterator.hasNext()) {
            RejectedExecutionHandler rejectedExecutionHandler = iterator.next();
            String rejectedExecutionHandlerName = rejectedExecutionHandler.getClass().getSimpleName();
            if (rejectedExecutionType.equals(rejectedExecutionHandlerName)) {
                return rejectedExecutionHandler;
            }
        }
        return new BqAbortPolicy(threadPoolName);
    }

    /**
     * 获取阻塞队列
     * @param queueType
     * @param queueCapacity
     * @param fair
     * @return
     */
    private BlockingQueue getBlockingQueue(String queueType, int queueCapacity, boolean fair) {
        if (!QueueTypeEnum.exists(queueType)) {
            throw new RuntimeException("队列不存在 " + queueType);
        }
        if (QueueTypeEnum.ARRAY_BLOCKING_QUEUE.getType().equals(queueType)) {
            return new ArrayBlockingQueue(queueCapacity);
        }
        if (QueueTypeEnum.SYNCHRONOUS_QUEUE.getType().equals(queueType)) {
            return new SynchronousQueue(fair);
        }
        if (QueueTypeEnum.PRIORITY_BLOCKING_QUEUE.getType().equals(queueType)) {
            return new PriorityBlockingQueue(queueCapacity);
        }
        if (QueueTypeEnum.DELAY_QUEUE.getType().equals(queueType)) {
            return new DelayQueue();
        }
        if (QueueTypeEnum.LINKED_BLOCKING_DEQUE.getType().equals(queueType)) {
            return new LinkedBlockingDeque(queueCapacity);
        }
        if (QueueTypeEnum.LINKED_TRANSFER_DEQUE.getType().equals(queueType)) {
            return new LinkedTransferQueue();
        }
        return new ResizableCapacityLinkedBlockIngQueue(queueCapacity);
    }

    /**
     * 刷新线程池
     */
    public void refreshThreadPoolExecutor(boolean isWaitConfigRefreshOver) {
        try {
            if (isWaitConfigRefreshOver) {
                // 等待Nacos配置刷新完成
                Thread.sleep(dynamicThreadPoolProperties.getNacosWaitRefreshConfigSeconds() * 1000);
            }
        } catch (InterruptedException e) {

        }
        dynamicThreadPoolProperties.getExecutors().forEach(executor -> {
            ThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(executor.getThreadPoolName());
            threadPoolExecutor.setCorePoolSize(executor.getCorePoolSize());
            threadPoolExecutor.setMaximumPoolSize(executor.getMaximumPoolSize());
            threadPoolExecutor.setKeepAliveTime(executor.getKeepAliveTime(), executor.getUnit());
            threadPoolExecutor.setRejectedExecutionHandler(getRejectedExecutionHandler(executor.getRejectedExecutionType(), executor.getThreadPoolName()));
            BlockingQueue<Runnable> queue = threadPoolExecutor.getQueue();
            if (queue instanceof ResizableCapacityLinkedBlockIngQueue) {
                ((ResizableCapacityLinkedBlockIngQueue<Runnable>) queue).setCapacity(executor.getQueueCapacity());
            }
        });
    }

    public BqThreadPoolExecutor getThreadPoolExecutor(String threadPoolName) {
        BqThreadPoolExecutor threadPoolExecutor = threadPoolExecutorMap.get(threadPoolName);
        if (threadPoolExecutor == null) {
            throw new NullPointerException("找不到线程池 " + threadPoolName);
        }
        return threadPoolExecutor;
    }

    public AtomicLong getRejectCount(String threadPoolName) {
        return threadPoolExecutorRejectCountMap.get(threadPoolName);
    }

    public void clearRejectCount(String threadPoolName) {
        threadPoolExecutorRejectCountMap.remove(threadPoolName);
    }

    public Map<String, BqThreadPoolExecutor> getThreadPoolExecutorMap() {
        return threadPoolExecutorMap;
    }

    static class BqThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        BqThreadFactory(String threadPoolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = threadPoolName + "-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    static class BqAbortPolicy implements RejectedExecutionHandler {

        private String threadPoolName;

        /**
         * Creates an {@code AbortPolicy}.
         */
        public BqAbortPolicy() { }

        public BqAbortPolicy(String threadPoolName) {
            this.threadPoolName = threadPoolName;
        }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            AtomicLong atomicLong=threadPoolExecutorRejectCountMap.get(threadPoolName);
            if(atomicLong==null){
                atomicLong=new AtomicLong(0);
            }
            BqThreadPoolExecutor executor=threadPoolExecutorMap.get(threadPoolName);
            executor.getTaskRuntime().remove(String.valueOf(r.hashCode()));
            atomicLong.incrementAndGet();
            if (atomicLong != null) {
                threadPoolExecutorRejectCountMap.put(threadPoolName, atomicLong);;
            }
//            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
            log.error("reject task");
        }
    }

}
