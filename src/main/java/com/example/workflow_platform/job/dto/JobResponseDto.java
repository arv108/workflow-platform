package com.example.workflow_platform.job.dto;

public record JobResponseDto(
        Long id,
        Long workflowId,
        String name,
        String type,
        int maxRetries,
        Integer timeoutSeconds
) {}