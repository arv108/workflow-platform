CREATE TABLE workflow (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);


CREATE TABLE job (
    id BIGSERIAL PRIMARY KEY,

    workflow_id BIGINT NOT NULL,

    name VARCHAR(255) NOT NULL,

    job_type VARCHAR(50) NOT NULL,

    configuration JSONB,

    max_retries INT NOT NULL DEFAULT 3,

    timeout_seconds INT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_job_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflow(id)
);


CREATE TABLE job_dependency (
    id BIGSERIAL PRIMARY KEY,

    job_id BIGINT NOT NULL,

    depends_on_job_id BIGINT NOT NULL,

    CONSTRAINT uk_job_dependency
        UNIQUE (job_id, depends_on_job_id),

    CONSTRAINT fk_dependency_job
        FOREIGN KEY (job_id)
        REFERENCES job(id),

    CONSTRAINT fk_dependency_parent
        FOREIGN KEY (depends_on_job_id)
        REFERENCES job(id),

    CONSTRAINT chk_no_self_dependency
        CHECK (job_id <> depends_on_job_id)
);


CREATE TABLE workflow_execution (
    id BIGSERIAL PRIMARY KEY,

    workflow_id BIGINT NOT NULL,

    status VARCHAR(50) NOT NULL,

    started_at TIMESTAMP,

    completed_at TIMESTAMP,

    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_execution_workflow
        FOREIGN KEY (workflow_id)
        REFERENCES workflow(id)
);


CREATE TABLE job_execution (
    id BIGSERIAL PRIMARY KEY,

    job_id BIGINT NOT NULL,

    workflow_execution_id BIGINT NOT NULL,

    status VARCHAR(50) NOT NULL,

    attempt INT NOT NULL DEFAULT 0,

    worker_id VARCHAR(255),

    lease_expiry TIMESTAMP,

    started_at TIMESTAMP,

    completed_at TIMESTAMP,

    error_message TEXT,

    created_at TIMESTAMP NOT NULL,

    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_job_execution_job
        FOREIGN KEY (job_id)
        REFERENCES job(id),

    CONSTRAINT fk_job_execution_workflow_execution
        FOREIGN KEY (workflow_execution_id)
        REFERENCES workflow_execution(id)
);