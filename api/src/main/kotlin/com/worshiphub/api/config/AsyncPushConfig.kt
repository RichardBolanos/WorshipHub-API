package com.worshiphub.api.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * Async and retry configuration for background tasks (push notifications, emails).
 *
 * Provides dedicated ThreadPoolTaskExecutors so that I/O-bound operations
 * (FCM, SMTP) run on isolated thread pools and never block the request thread.
 *
 * Usage:
 *   @Async("pushNotificationExecutor")  → for FCM push delivery
 *   @Async("emailExecutor")             → for SMTP email delivery
 *   @Async                              → falls back to the default executor below
 */
@Configuration
@EnableAsync
@EnableRetry
class AsyncPushConfig {

    private val logger = LoggerFactory.getLogger(AsyncPushConfig::class.java)

    @Bean("pushNotificationExecutor")
    fun pushNotificationExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 5
        executor.queueCapacity = 100
        executor.setThreadNamePrefix("push-notif-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(30)
        executor.initialize()

        logger.info(
            "Push notification executor configured — core={}, max={}, queue={}",
            executor.corePoolSize, executor.maxPoolSize, executor.queueCapacity
        )

        return executor
    }

    /**
     * Default executor used by `@Async` (without a name argument). Emails
     * use this pool — small but bounded so a slow SMTP provider can't exhaust
     * threads.
     */
    @Bean("taskExecutor")
    fun emailExecutor(): Executor {
        val executor = ThreadPoolTaskExecutor()
        executor.corePoolSize = 2
        executor.maxPoolSize = 4
        executor.queueCapacity = 50
        executor.setThreadNamePrefix("email-")
        executor.setWaitForTasksToCompleteOnShutdown(true)
        executor.setAwaitTerminationSeconds(15)
        executor.initialize()

        logger.info(
            "Email executor configured — core={}, max={}, queue={}",
            executor.corePoolSize, executor.maxPoolSize, executor.queueCapacity
        )

        return executor
    }
}
