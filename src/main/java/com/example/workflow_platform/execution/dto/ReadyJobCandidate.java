package com.example.workflow_platform.execution.dto;

public record ReadyJobCandidate(
        Long workflowExecutionId,
        Long jobId
) {
}
