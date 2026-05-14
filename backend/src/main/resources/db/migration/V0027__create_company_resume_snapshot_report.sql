CREATE TABLE company_resume_snapshot_report (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT UNSIGNED NOT NULL,
  reporter_user_id BIGINT UNSIGNED NOT NULL,
  snapshot_id BIGINT UNSIGNED NOT NULL,
  reason_code VARCHAR(64) NOT NULL,
  remark_summary VARCHAR(300) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_company_report_time (company_id, created_at),
  KEY idx_snapshot_status_time (snapshot_id, status, created_at),
  KEY idx_reason_status_time (reason_code, status, created_at),
  CONSTRAINT fk_resume_snapshot_report_company FOREIGN KEY (company_id) REFERENCES company(id),
  CONSTRAINT fk_resume_snapshot_report_user FOREIGN KEY (reporter_user_id) REFERENCES company_user(id),
  CONSTRAINT fk_resume_snapshot_report_snapshot FOREIGN KEY (snapshot_id) REFERENCES candidate_publish_snapshot(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
