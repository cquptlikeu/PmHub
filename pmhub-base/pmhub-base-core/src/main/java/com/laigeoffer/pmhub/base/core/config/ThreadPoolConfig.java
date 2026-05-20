package com.laigeoffer.pmhub.base.core.config;

import com.laigeoffer.pmhub.base.core.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置
 *
 * @author canghe
 **/
@Configuration
public class ThreadPoolConfig {
    // 核心线程池大小,线程池中保持活跃的最小线程数,即使空闲也不会被回收，除非设置了 allowCoreThreadTimeOut
    private int corePoolSize = 50;

    // 线程池能创建的最大线程数量,当任务队列满了之后，才会创建超过核心线程数的线程
    private int maxPoolSize = 200;

    // 任务队列的最大长度,当核心线程都在忙时，新任务进入队列等待
    private int queueCapacity = 1000;

    // 非核心线程空闲多久后被回收
    private int keepAliveSeconds = 300;

    /**
     * ThreadPoolTaskExecutor就是 Spring 对 Java 原生线程池的一个高级封装（包装器）。
     * 它不仅完全具备原生线程池的能力，还与 Spring 的生态（比如 Bean 的生命周期、@Async 异步注解）进行了深度整合，让开发者在 Spring Boot 项目中使用多线程变得非常简单和优雅。
     *
     * 线程池的工作机制
     *       新任务到达
     *       ↓
     *   1. 核心线程有空闲？
     *      → 是：核心线程执行
     *      → 否：继续 ↓
     *       ↓
     *   2. 任务队列未满？
     *      → 是：任务入队（核心线程会从队列取任务执行）
     *      → 否：继续 ↓
     *       ↓
     *   3. 当前线程数 < maxPoolSize？
     *      → 是：创建【非核心线程】处理【当前这个溢出的任务】
     *      → 否：继续 ↓
     *       ↓
     *   4. 触发拒绝策略
     *
     *   关键点： 非核心线程的创建时机：当队列已满且还有新任务溢出时，才会创建非核心线程。
     *   非核心线程执行什么：
     *   1. 创建时：直接处理导致队列满的那个"溢出任务"
     *   2. 创建后：会从队列中获取其他等待的任务执行
     *
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程池对拒绝任务(无线程可用)的处理策略
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 执行周期性或定时任务
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService() {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy()) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                Threads.printException(r, t);
            }
        };
    }
}
