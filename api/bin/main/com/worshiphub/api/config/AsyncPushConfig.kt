package com.worshiphub.api.config

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.annotation.EnableRetry
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor

/**
 * Async and retry configuration for push notification delivery.
 *
 * Provides a dedicated ThreadPoolTaskExecutor named "pushNotificationExecutor"
 * so that push notification sends run on a separate thread pool and do not
 * block the caller's request thread.
 *
 * Usage: annotate service methods with @Async("pushNotificationExecutor").
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
}
