ALTER TABLE interview_session
  ADD COLUMN application_id BIGINT UNSIGNED DEFAULT NULL AFTER id,
  ADD COLUMN signin_code_hash VARCHAR(128) DEFAULT NULL AFTER qr_code,
  ADD COLUMN signin_code_expires_at DATETIME DEFAULT NULL AFTER signin_code_hash,
  ADD COLUMN signin_code_used_at DATETIME DEFAULT NULL AFTER signin_code_expires_at,
  ADD UNIQUE KEY uk_session_application (application_id),
  ADD UNIQUE KEY uk_session_signin_code_hash (signin_code_hash),
  ADD CONSTRAINT fk_session_application FOREIGN KEY (application_id) REFERENCES job_application(id);
