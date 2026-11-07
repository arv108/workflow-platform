package com.example.workflow_platform.execution.dto;

import java.time.LocalDateTime;

public record JobFailureResponse(
        Long executionId,
        Long jobId,
        int attempt,
        String status,
        LocalDateTime nextRetryAt,
        String message
) {
}
