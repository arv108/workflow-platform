package com.example.workflow_platform.execution.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class WorkerPoolConfig {

    @Bean(name = "jobWorkerExecutor", destroyMethod = "shutdown")
    public ExecutorService jobWorkerExecutor(
            @Value("${workflow.worker.pool-size:3}") int poolSize) {

        if (poolSize < 1) {
            throw new IllegalArgumentException(
                    "Worker pool size must be greater than zero"
            );
        }

        AtomicInteger counter = new AtomicInteger(1);

        ThreadFactory threadFactory = task -> {
            Thread thread = new Thread(task);
            thread.setName(
                    "job-worker-thread-" + counter.getAndIncrement()
            );
            return thread;
        };

        return Executors.newFixedThreadPool(
                poolSize,
                threadFactory
        );
    }
}
