package com.example.workflow_platform.execution.controller;

import com.example.workflow_platform.execution.dto.WorkflowExecutionResponseDto;
import com.example.workflow_platform.execution.service.WorkflowExecutionService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowExecutionController {

    private final WorkflowExecutionService workflowExecutionService;

    public WorkflowExecutionController(
            WorkflowExecutionService workflowExecutionService) {
        this.workflowExecutionService = workflowExecutionService;
    }

    @PostMapping("/{workflowId}/executions")
    public ResponseEntity<WorkflowExecutionResponseDto> startExecution(
            @PathVariable Long workflowId) {

        WorkflowExecutionResponseDto response =
                workflowExecutionService.startExecution(workflowId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}