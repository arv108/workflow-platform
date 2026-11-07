package com.example.workflow_platform.execution.scheduler;

import com.example.workflow_platform.execution.service.JobExecutionService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JobLeaseRecoveryScheduler {

    private final JobExecutionService jobExecutionService;

    public JobLeaseRecoveryScheduler(
            JobExecutionService jobExecutionService) {
        this.jobExecutionService = jobExecutionService;
    }

    @Scheduled(fixedDelayString =
            "${workflow.worker.recovery-poll-interval-ms:5000}")
    public void recoverExpiredLeases() {

        // Process a maximum of 50 expired jobs per polling cycle.
        for (int i = 0; i < 50; i++) {

            boolean recovered =
                    jobExecutionService.recoverOneExpiredJob();

            if (!recovered) {
                break;
            }
        }
    }
}
