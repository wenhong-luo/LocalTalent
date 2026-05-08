ALTER TABLE candidate_resume
  ADD COLUMN attachment_file_name VARCHAR(180) DEFAULT NULL AFTER attachment_object_key,
  ADD COLUMN attachment_content_type VARCHAR(120) DEFAULT NULL AFTER attachment_file_name,
  ADD COLUMN attachment_size_bytes BIGINT UNSIGNED DEFAULT NULL AFTER attachment_content_type,
  ADD COLUMN attachment_uploaded_at DATETIME DEFAULT NULL AFTER attachment_size_bytes,
  ADD COLUMN attachment_sha256 CHAR(64) DEFAULT NULL AFTER attachment_uploaded_at,
  ADD KEY idx_candidate_resume_attachment_time (candidate_id, attachment_uploaded_at);
