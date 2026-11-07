package com.example.workflow_platform.job.dto;

import jakarta.validation.constraints.NotNull;

public record CreateJobDependencyRequest(
        @NotNull Long dependsOnJobId
) {
}