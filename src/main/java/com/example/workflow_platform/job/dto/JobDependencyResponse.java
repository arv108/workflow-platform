package com.example.workflow_platform.job.dto;

public record JobDependencyResponse(
        Long jobId,
        Long dependsOnJobId
) {
}