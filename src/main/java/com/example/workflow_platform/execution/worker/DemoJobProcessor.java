package com.example.workflow_platform.execution.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DemoJobProcessor {

    private static final Logger log =
            LoggerFactory.getLogger(DemoJobProcessor.class);

    public void execute(
            Long workflowExecutionId,
            Long jobId) throws InterruptedException {

        log.info(
                "Starting job {} in execution {} on thread {}",
                jobId,
                workflowExecutionId,
                Thread.currentThread().getName()
        );

        // Simulate actual business processing.
        Thread.sleep(2000);

        log.info(
                "Finished job {} in execution {} on thread {}",
                jobId,
                workflowExecutionId,
                Thread.currentThread().getName()
        );
    }
}
