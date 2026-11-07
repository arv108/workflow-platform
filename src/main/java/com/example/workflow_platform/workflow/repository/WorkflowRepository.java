package com.example.workflow_platform.workflow.repository;

import com.example.workflow_platform.workflow.entity.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowRepository
        extends JpaRepository<Workflow, Long> {
}
