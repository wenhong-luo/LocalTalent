CREATE TABLE candidate_resume_onboarding (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  resume_id BIGINT UNSIGNED DEFAULT NULL,
  onboarding_status VARCHAR(32) NOT NULL DEFAULT 'not_started',
  current_step VARCHAR(32) NOT NULL DEFAULT 'basic',
  completion_score INT NOT NULL DEFAULT 0,
  started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  completed_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_candidate_resume_onboarding_candidate (candidate_id),
  KEY idx_candidate_resume_onboarding_status (onboarding_status, current_step, updated_at),
  KEY idx_candidate_resume_onboarding_resume (resume_id),
  CONSTRAINT fk_resume_onboarding_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id),
  CONSTRAINT fk_resume_onboarding_resume FOREIGN KEY (resume_id) REFERENCES candidate_resume(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
