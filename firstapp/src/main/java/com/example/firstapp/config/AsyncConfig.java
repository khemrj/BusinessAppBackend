package com.example.firstapp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent
        .ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Async Configuration.
 *
 * Single Responsibility:
 * ONLY configures the thread pool used for async operations.
 *
 * WHY a dedicated thread pool for email?
 * → HTTP request threads and email threads are separated
 * → Slow SMTP server never blocks user-facing responses
 * → Pool size limits concurrent SMTP connections
 * → Graceful shutdown waits for queued emails to finish
 *
 * Without this config:
 * Spring uses SimpleAsyncTaskExecutor which creates
 * a new thread for EVERY async call = uncontrolled
 * thread growth = possible OutOfMemoryError under load
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

    /**
     * Dedicated thread pool for email sending.
     *
     * Named "emailTaskExecutor" — must match
     * @Async("emailTaskExecutor") in EmailServiceImpl.
     *
     * SIZING for Phase 1 (Brevo free — 300 emails/day):
     * Core: 2 threads always ready
     * Max: 5 threads under load
     * Queue: 50 emails can wait before rejection
     *
     * 300 emails/day = ~12 emails/hour = 1 per 5 minutes
     * 2 core threads is more than sufficient
     */
    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor =
            new ThreadPoolTaskExecutor();

        // Always-alive threads — immediately available
        executor.setCorePoolSize(2);

        // Max concurrent email sends
        // Each = one active SMTP connection to Brevo
        executor.setMaxPoolSize(5);

        // Queue before rejecting
        // 50 emails can wait if all 5 threads are busy
        executor.setQueueCapacity(50);

        // Thread name prefix — visible in logs for debugging
        // Makes it easy to identify email threads
        // "email-thread-1 sending to kj@gmail.com"
        executor.setThreadNamePrefix("email-thread-");

        // On application shutdown:
        // Don't kill threads mid-send
        // Wait for in-flight emails to complete
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // Maximum wait time during shutdown
        // After 30 seconds: force shutdown regardless
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info(
            "Email thread pool initialized: " +
            "core={} max={} queue={}",
            2, 5, 50
        );

        return executor;
    }
}
