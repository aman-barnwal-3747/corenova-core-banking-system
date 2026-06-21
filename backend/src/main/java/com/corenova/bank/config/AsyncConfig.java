package com.corenova.bank.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * ================================================================
 *  AsyncConfig – Thread Pool Configuration for @Async Operations
 *
 *  Configures dedicated thread pools for non-blocking operations:
 *
 *  1. auditExecutor       : Audit log writes (high-volume, low-priority)
 *  2. notificationExecutor: SMS/email notifications (I/O bound)
 *
 *  Thread pool sizing (rule of thumb for I/O-bound tasks):
 *    Pool Size = CPU cores × (1 + wait-time/compute-time)
 *    For I/O ratio ~10:1 on 4-core machine → 4 × 11 ≈ 44 threads
 *
 *  In production: externalize pool sizes to application.yml
 *  and use @Value injection for dynamic tuning.
 * ================================================================
 */
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /**
     * Thread pool for asynchronous audit log writes.
     *
     * Configuration:
     *  • Core:    5 threads (always alive)
     *  • Max:     20 threads (burst capacity)
     *  • Queue:   500 tasks (backlog before rejection)
     *  • Name:    "audit-exec-N" (visible in thread dumps)
     *
     * If queue overflows: CallerRunsPolicy kicks in — the calling
     * thread executes the audit write synchronously (acceptable).
     */
    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-exec-");
        executor.setRejectedExecutionHandler(
            new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
        );
        executor.initialize();
        return executor;
    }

    /**
     * Thread pool for notification dispatch (SMS/email).
     * Larger pool since notification calls are more I/O-bound.
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(30);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("notify-exec-");
        executor.initialize();
        return executor;
    }

    /** Default executor for other @Async methods. */
    @Override
    public Executor getAsyncExecutor() {
        return auditExecutor();
    }
}
