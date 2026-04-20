CREATE TABLE IF NOT EXISTS process (
    process_id VARCHAR(255) PRIMARY KEY,
    status VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    objective VARCHAR(255) NOT NULL,
    result_kind VARCHAR(255) NOT NULL,
    pause_requested BOOLEAN NOT NULL,
    stop_requested BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS process_plan (
    plan_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL UNIQUE,
    source_folder VARCHAR(255) NOT NULL,
    selection_mode VARCHAR(255) NOT NULL,
    selected_files VARCHAR(4000) NOT NULL,
    total_planned_files INTEGER NOT NULL,
    batch_size INTEGER NOT NULL,
    summary_policy VARCHAR(255) NOT NULL,
    failure_policy VARCHAR(255) NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_process_plan_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS authorization_info (
    authorization_info_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL UNIQUE,
    authorization_required BOOLEAN NOT NULL,
    authorization_state VARCHAR(255) NOT NULL,
    pending_reason VARCHAR(255),
    authorized_at TIMESTAMP(6) WITH TIME ZONE,
    authorization_note VARCHAR(255),
    last_authorization_update_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_authorization_info_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS progress_snapshot (
    progress_snapshot_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL UNIQUE,
    total_files INTEGER NOT NULL,
    processed_files INTEGER NOT NULL,
    successful_files INTEGER NOT NULL,
    failed_files INTEGER NOT NULL,
    pending_files INTEGER NOT NULL,
    percentage DOUBLE PRECISION NOT NULL,
    current_batch_index INTEGER,
    current_batch_size INTEGER,
    started_at TIMESTAMP(6) WITH TIME ZONE,
    estimated_completion TIMESTAMP(6) WITH TIME ZONE,
    last_progress_at TIMESTAMP(6) WITH TIME ZONE,
    CONSTRAINT fk_progress_snapshot_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS execution_control_flags (
    process_id VARCHAR(255) PRIMARY KEY,
    pause_requested BOOLEAN NOT NULL,
    stop_requested BOOLEAN NOT NULL,
    last_control_command_at TIMESTAMP(6) WITH TIME ZONE,
    last_control_command_type VARCHAR(255),
    CONSTRAINT fk_execution_control_flags_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS terminal_info (
    terminal_info_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL UNIQUE,
    terminal_state VARCHAR(255) NOT NULL,
    terminal_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    terminal_reason_code VARCHAR(255) NOT NULL,
    terminal_reason_message VARCHAR(255),
    final_coverage DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_terminal_info_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS document_execution (
    document_execution_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    document_name VARCHAR(255) NOT NULL,
    document_path VARCHAR(255) NOT NULL,
    document_status VARCHAR(255) NOT NULL,
    batch_index INTEGER NOT NULL,
    started_at TIMESTAMP(6) WITH TIME ZONE,
    finished_at TIMESTAMP(6) WITH TIME ZONE,
    word_count INTEGER,
    line_count INTEGER,
    character_count INTEGER,
    most_frequent_words_json CLOB,
    summary_text CLOB,
    summary_method VARCHAR(255),
    error_code VARCHAR(255),
    error_message VARCHAR(255),
    CONSTRAINT fk_document_execution_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS process_result (
    process_result_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    result_kind VARCHAR(255) NOT NULL,
    computed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    included_documents_json CLOB NOT NULL,
    excluded_documents_json CLOB NOT NULL,
    total_words BIGINT NOT NULL,
    total_lines BIGINT NOT NULL,
    total_characters BIGINT NOT NULL,
    most_frequent_words_json CLOB NOT NULL,
    document_summaries_json CLOB NOT NULL,
    global_summary CLOB,
    planned_files INTEGER NOT NULL,
    included_files INTEGER NOT NULL,
    excluded_files INTEGER NOT NULL,
    coverage_percentage DOUBLE PRECISION NOT NULL,
    is_final BOOLEAN NOT NULL,
    CONSTRAINT fk_process_result_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS activity_log (
    activity_id VARCHAR(255) PRIMARY KEY,
    process_id VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    event_stage VARCHAR(255) NOT NULL,
    message VARCHAR(255) NOT NULL,
    metadata_json CLOB,
    correlation_id VARCHAR(255),
    CONSTRAINT fk_activity_log_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE TABLE IF NOT EXISTS process_lease (
    process_id VARCHAR(255) PRIMARY KEY,
    owner_id VARCHAR(255) NOT NULL,
    lease_until TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_process_lease_process
        FOREIGN KEY (process_id) REFERENCES process(process_id)
);

CREATE INDEX IF NOT EXISTS idx_process_status_updated_at
    ON process (status, updated_at);

CREATE INDEX IF NOT EXISTS idx_document_execution_process_batch_name
    ON document_execution (process_id, batch_index, document_name);

CREATE INDEX IF NOT EXISTS idx_document_execution_process_status
    ON document_execution (process_id, document_status);

CREATE INDEX IF NOT EXISTS idx_document_execution_status_process_batch_name
    ON document_execution (document_status, process_id, batch_index, document_name);

CREATE INDEX IF NOT EXISTS idx_process_result_process_computed_at
    ON process_result (process_id, computed_at);

CREATE INDEX IF NOT EXISTS idx_activity_log_process_timestamp
    ON activity_log (process_id, timestamp);

CREATE INDEX IF NOT EXISTS idx_activity_log_process_event_type_timestamp
    ON activity_log (process_id, event_type, timestamp);

CREATE INDEX IF NOT EXISTS idx_process_lease_lease_until
    ON process_lease (lease_until);
