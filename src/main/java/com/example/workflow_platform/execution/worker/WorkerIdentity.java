package com.example.workflow_platform.execution.worker;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class WorkerIdentity {

    private final String workerId =
            "worker-" + UUID.randomUUID();

    public String getWorkerId() {
        return workerId;
    }
}