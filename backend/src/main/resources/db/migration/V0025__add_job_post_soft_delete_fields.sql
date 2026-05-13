ALTER TABLE job_post
    ADD COLUMN deleted_at DATETIME NULL AFTER status_changed_at,
    ADD COLUMN deleted_by BIGINT NULL AFTER deleted_at,
    ADD COLUMN delete_reason VARCHAR(500) NULL AFTER deleted_by;

CREATE INDEX idx_job_post_company_deleted_updated
    ON job_post (company_id, deleted_at, updated_at);

CREATE INDEX idx_job_post_public_deleted_visible
    ON job_post (deleted_at, status, audit_status, city_code, category_code, updated_at);
