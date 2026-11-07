package com.example.workflow_platform.execution.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CompleteJobRequest(
        @NotBlank String workerId,
        @NotNull Long fencingToken
) {
}
