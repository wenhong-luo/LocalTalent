ALTER TABLE company
  ADD COLUMN auth_reject_reason VARCHAR(500) DEFAULT NULL AFTER auth_status,
  ADD COLUMN auth_review_user_id BIGINT UNSIGNED DEFAULT NULL AFTER auth_reject_reason,
  ADD COLUMN auth_review_time DATETIME DEFAULT NULL AFTER auth_review_user_id,
  ADD COLUMN auth_submit_time DATETIME DEFAULT NULL AFTER auth_review_time;

ALTER TABLE job_post
  ADD COLUMN review_memo VARCHAR(500) DEFAULT NULL AFTER audit_status,
  ADD COLUMN reject_reason VARCHAR(500) DEFAULT NULL AFTER review_memo,
  ADD COLUMN review_user_id BIGINT UNSIGNED DEFAULT NULL AFTER reject_reason,
  ADD COLUMN review_time DATETIME DEFAULT NULL AFTER review_user_id,
  ADD COLUMN offline_reason VARCHAR(500) DEFAULT NULL AFTER published_at,
  ADD COLUMN status_changed_at DATETIME DEFAULT NULL AFTER offline_reason;
