package com.example.workflow_platform.job.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record JobRequestDto(
        @NotBlank String name,
        @NotBlank String type,
        @NotNull @Min(0) Integer maxRetries,
        @Positive Integer timeoutSeconds
) {}