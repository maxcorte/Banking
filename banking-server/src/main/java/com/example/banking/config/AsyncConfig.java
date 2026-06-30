package com.example.banking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Active le traitement asynchrone et fournit un exécuteur dédié, BORNÉ, pour les
 * notifications. Borné (max 4 threads) afin de ne jamais saturer le pool de
 * connexions JDBC : au pire 4 écritures de notifications en parallèle.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("notif-");
        executor.initialize();
        return executor;
    }
}
