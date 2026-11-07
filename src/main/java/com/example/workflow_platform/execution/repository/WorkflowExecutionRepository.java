package com.example.workflow_platform.execution.repository;

import com.example.workflow_platform.execution.entity.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowExecutionRepository
        extends JpaRepository<WorkflowExecution, Long> {
}
