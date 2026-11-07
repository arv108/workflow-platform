package com.example.workflow_platform.workflow.dto;

public record WorkflowResponseDto(
        Long id,
        String name,
        String description,
        String status
) {}