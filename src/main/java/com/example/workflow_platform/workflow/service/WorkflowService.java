package com.example.workflow_platform.workflow.service;

import com.example.workflow_platform.workflow.dto.CreateWorkflowRequest;
import com.example.workflow_platform.workflow.dto.WorkflowResponseDto;
import com.example.workflow_platform.workflow.entity.Workflow;
import com.example.workflow_platform.workflow.enums.WorkflowStatus;
import com.example.workflow_platform.workflow.repository.WorkflowRepository;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkflowService {

    private final WorkflowRepository workflowRepository;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    public WorkflowService(WorkflowRepository workflowRepository) {
        this.workflowRepository = workflowRepository;
    }

    @Transactional
    public WorkflowResponseDto createWorkflow(
            CreateWorkflowRequest request) {

        Workflow workflow = new Workflow();
        workflow.setName(request.name());
        workflow.setDescription(request.description());
        workflow.setStatus(WorkflowStatus.ACTIVE);

        Workflow saved = workflowRepository.save(workflow);

        return new WorkflowResponseDto(
                saved.getId(),
                saved.getName(),
                saved.getDescription(),
                saved.getStatus().name()
        );
    }
}