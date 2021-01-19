package com.h2t.study.threadpool;

import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ReflectiveMethodInvocation;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;

public class BqThreadPoolExecutor extends ThreadPoolExecutor {

    private static Logger logger = LoggerFactory.getLogger(ThreadPoolExecutor.class);

    /**
     * 线程池名称
     */
    private String threadPoolName;

    private String defaultTaskName = "defaultTask";

    /**
     * The default rejected execution handler
     */
    private static final RejectedExecutionHandler defaultHandler = new ThreadPoolExecutor.AbortPolicy();



    private Map<String, String> runnableNameMap = new ConcurrentHashMap<>();

    private Map<String, Long> taskRuntime = new ConcurrentHashMap<>();

    public BqThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public BqThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                   BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
    }

    public BqThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit,
                                   BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler, String threadPoolName) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.threadPoolName = threadPoolName;
    }

    @Override
    public void execute(Runnable command) {
        super.execute(command);
    }

    public void execute(Runnable command, String taskName) {
        super.execute(command);
    }

    public Future<?> submit(Runnable task, String taskName) {
        return super.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task, String taskName) {
        return super.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result, String taskName) {
        return super.submit(task, result);
    }

    public Future<?> submit(Runnable task) {
        return super.submit(task);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(task, result);
    }

    @SneakyThrows
    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        taskRuntime.putIfAbsent(String.valueOf(r.hashCode()),System.currentTimeMillis());
        Field field= null;
        String taskName="";
        try {
            Field callble=r.getClass().getDeclaredField("callable");
            callble.setAccessible(true);
            Callable task= (Callable) callble.get(r);
            field = task.getClass().getDeclaredField("arg$2");
            field.setAccessible(true);
            ReflectiveMethodInvocation invocation= (ReflectiveMethodInvocation) field.get(task);
            taskName=invocation.getMethod().getName();
        } catch (NoSuchFieldException e) {
            logger.error("获取任务方法名异常",e);
        } catch (IllegalAccessException e) {
            logger.error("获取任务方法名异常",e);
        }
        runnableNameMap.put(String.valueOf(r.hashCode()),taskName);
        super.beforeExecute(t, r);
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        long taskEndTime=System.currentTimeMillis();
        long taskExecTime=taskEndTime-taskRuntime.get(String.valueOf(r.hashCode()));

        String taskName=runnableNameMap.get(String.valueOf(r.hashCode()));
        logger.info("task:{} execTime:{}",taskName,taskExecTime);
        runnableNameMap.remove(String.valueOf(r.hashCode()));
        taskRuntime.remove(String.valueOf(r.hashCode()));
    }

    public Map<String, Long> getTaskRuntime() {
        return taskRuntime;
    }

    public void setTaskRuntime(Map<String, Long> taskRuntime) {
        this.taskRuntime = taskRuntime;
    }
}
