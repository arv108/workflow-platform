package com.example.workflow_platform.execution.dto;

public record WorkflowExecutionResponseDto(
        Long executionId,
        Long workflowId,
        String status
) {
}