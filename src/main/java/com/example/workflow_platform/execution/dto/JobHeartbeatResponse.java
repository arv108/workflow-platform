package com.example.workflow_platform.execution.dto;

import java.time.LocalDateTime;

public record JobHeartbeatResponse(
        Long executionId,
        Long jobId,
        String workerId,
        Long fencingToken,
        LocalDateTime leaseExpiry,
        String status
) {
}
