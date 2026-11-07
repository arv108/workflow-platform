package com.example.workflow_platform.workflow.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWorkflowRequest(
        @NotBlank String name,
        String description
) {}