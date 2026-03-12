-- V2__project_data_registry.sql
-- Tracks dynamically created project tables

CREATE TABLE project_data_tables (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id   UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    table_name   VARCHAR(200) NOT NULL,   -- actual PG table name e.g. proj_abc123_users
    logical_name VARCHAR(100) NOT NULL,   -- original name from schema JSON e.g. users
    columns      JSONB NOT NULL,          -- ["id","name","email","created_at"]
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE (project_id, logical_name)
);

CREATE INDEX idx_project_data_tables_project_id ON project_data_tables(project_id);