package com.example.workflow_platform.workflow.controller;

import com.example.workflow_platform.workflow.dto.CreateWorkflowRequest;
import com.example.workflow_platform.workflow.dto.WorkflowResponseDto;
import com.example.workflow_platform.workflow.service.WorkflowService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping
    public ResponseEntity<WorkflowResponseDto> createWorkflow(
            @Valid @RequestBody CreateWorkflowRequest request) {

        WorkflowResponseDto response =
                workflowService.createWorkflow(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}