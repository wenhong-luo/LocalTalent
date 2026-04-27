CREATE TABLE activity_event (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  type_code VARCHAR(64) NOT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  location VARCHAR(255) DEFAULT NULL,
  organizer_company_id BIGINT UNSIGNED DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_city_time (city_code, start_time),
  KEY idx_status_time (status, start_time),
  CONSTRAINT fk_event_company FOREIGN KEY (organizer_company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE activity_registration (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  event_id BIGINT UNSIGNED NOT NULL,
  candidate_id BIGINT UNSIGNED NOT NULL,
  register_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  sign_status TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_event_candidate (event_id, candidate_id),
  KEY idx_candidate_event (candidate_id, event_id),
  CONSTRAINT fk_registration_event FOREIGN KEY (event_id) REFERENCES activity_event(id),
  CONSTRAINT fk_registration_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interview_session (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT UNSIGNED NOT NULL,
  company_id BIGINT UNSIGNED NOT NULL,
  event_id BIGINT UNSIGNED DEFAULT NULL,
  session_name VARCHAR(150) NOT NULL,
  session_time DATETIME NOT NULL,
  location VARCHAR(255) DEFAULT NULL,
  qr_code VARCHAR(255) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_company_time (company_id, session_time),
  KEY idx_job_time (job_id, session_time),
  CONSTRAINT fk_session_job FOREIGN KEY (job_id) REFERENCES job_post(id),
  CONSTRAINT fk_session_company FOREIGN KEY (company_id) REFERENCES company(id),
  CONSTRAINT fk_session_event FOREIGN KEY (event_id) REFERENCES activity_event(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE interview_signin (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  session_id BIGINT UNSIGNED NOT NULL,
  candidate_id BIGINT UNSIGNED NOT NULL,
  sign_channel VARCHAR(32) NOT NULL DEFAULT 'qrcode',
  sign_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  device_id VARCHAR(128) DEFAULT NULL,
  consent_redirect_flag TINYINT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_session_candidate (session_id, candidate_id),
  KEY idx_candidate_signin (candidate_id, sign_time),
  CONSTRAINT fk_signin_session FOREIGN KEY (session_id) REFERENCES interview_session(id),
  CONSTRAINT fk_signin_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_consent (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  consent_status TINYINT NOT NULL DEFAULT 0,
  consent_scope JSON NOT NULL,
  consent_version VARCHAR(32) NOT NULL,
  consent_channel VARCHAR(32) NOT NULL DEFAULT 'portal',
  consent_time DATETIME DEFAULT NULL,
  realname_verified_flag TINYINT NOT NULL DEFAULT 0,
  second_confirm_flag TINYINT NOT NULL DEFAULT 0,
  revoke_status TINYINT NOT NULL DEFAULT 0,
  revoke_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_candidate_consent_status (candidate_id, consent_status),
  KEY idx_revoke_status (revoke_status),
  CONSTRAINT fk_consent_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_publish_snapshot (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  legal_basis VARCHAR(64) NOT NULL DEFAULT 'consent',
  consent_status TINYINT NOT NULL DEFAULT 0,
  publishable_flag TINYINT NOT NULL DEFAULT 0,
  consent_version VARCHAR(32) DEFAULT NULL,
  visibility_scope TINYINT NOT NULL DEFAULT 2,
  snapshot_json JSON NOT NULL,
  sync_version INT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_publishable_status (publishable_flag, status),
  KEY idx_candidate_snapshot (candidate_id, status),
  KEY idx_visibility_scope (visibility_scope),
  CONSTRAINT fk_snapshot_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE cms_content (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  content_type VARCHAR(32) NOT NULL,
  title VARCHAR(200) NOT NULL,
  cover_url VARCHAR(500) DEFAULT NULL,
  summary VARCHAR(500) DEFAULT NULL,
  body_html MEDIUMTEXT DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  publish_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_type_status_time (content_type, status, publish_time),
  KEY idx_city_type (city_code, content_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_consent_evidence (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  consent_id BIGINT UNSIGNED NOT NULL,
  page_snapshot_uri VARCHAR(500) DEFAULT NULL,
  content_hash VARCHAR(128) DEFAULT NULL,
  device_id VARCHAR(128) DEFAULT NULL,
  ip VARCHAR(64) DEFAULT NULL,
  user_agent VARCHAR(500) DEFAULT NULL,
  sms_record_id VARCHAR(64) DEFAULT NULL,
  verify_vendor VARCHAR(32) DEFAULT NULL,
  verify_result_json JSON DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_consent_id (consent_id),
  CONSTRAINT fk_evidence_consent FOREIGN KEY (consent_id) REFERENCES candidate_consent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
