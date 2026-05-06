ALTER TABLE company
  ADD COLUMN certification_material_summary_json JSON DEFAULT NULL AFTER company_profile;

ALTER TABLE job_application
  ADD COLUMN company_stage_note VARCHAR(500) DEFAULT NULL AFTER application_status,
  ADD COLUMN stage_changed_by BIGINT UNSIGNED DEFAULT NULL AFTER company_stage_note,
  ADD COLUMN stage_changed_at DATETIME DEFAULT NULL AFTER stage_changed_by,
  ADD KEY idx_application_company_stage_time (application_status, stage_changed_at);
