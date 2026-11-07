package com.example.workflow_platform.execution.scheduler;

import com.example.workflow_platform.execution.service.JobExecutionService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobRetryScheduler {

    private final JobExecutionService jobExecutionService;

    public JobRetryScheduler(
            JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @Scheduled(fixedDelayString =
            "${workflow.retry.poll-interval-ms:3000}")
    public void processDueRetries() {

        jobExecutionService.promoteDueRetries();
    }
}