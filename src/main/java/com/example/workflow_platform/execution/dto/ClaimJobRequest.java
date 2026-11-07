package com.example.workflow_platform.execution.dto;

import jakarta.validation.constraints.NotBlank;

public record ClaimJobRequest(
        @NotBlank String workerId
) {
}