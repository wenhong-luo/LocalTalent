CREATE TABLE company (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_name VARCHAR(200) NOT NULL,
  license_no VARCHAR(64) NOT NULL,
  industry_code VARCHAR(64) DEFAULT NULL,
  scale_code VARCHAR(64) DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  address VARCHAR(255) DEFAULT NULL,
  company_profile TEXT DEFAULT NULL,
  auth_status TINYINT NOT NULL DEFAULT 1,
  source_system VARCHAR(32) NOT NULL DEFAULT 'portal',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_license_no (license_no),
  KEY idx_auth_status (auth_status),
  KEY idx_city_code (city_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE company_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT UNSIGNED NOT NULL,
  user_name VARCHAR(100) NOT NULL,
  role_code VARCHAR(64) NOT NULL,
  mobile VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  password_hash VARCHAR(255) NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_login_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_company_user_mobile (company_id, mobile),
  UNIQUE KEY uk_company_user_email (company_id, email),
  KEY idx_company_role (company_id, role_code),
  CONSTRAINT fk_company_user_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_user (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  mobile VARCHAR(32) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(64) DEFAULT NULL,
  realname_verified_flag TINYINT NOT NULL DEFAULT 0,
  register_channel VARCHAR(32) NOT NULL DEFAULT 'portal',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_candidate_user_mobile (mobile),
  UNIQUE KEY uk_candidate_user_email (email),
  KEY idx_register_channel (register_channel)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_resume (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  resume_name VARCHAR(150) NOT NULL,
  base_profile_json JSON DEFAULT NULL,
  education_json JSON DEFAULT NULL,
  experience_json JSON DEFAULT NULL,
  skills_json JSON DEFAULT NULL,
  attachment_object_key VARCHAR(500) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_candidate_resume_status (candidate_id, status),
  CONSTRAINT fk_resume_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_control_profile (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  legal_basis VARCHAR(64) NOT NULL DEFAULT 'consent',
  consent_status TINYINT NOT NULL DEFAULT 0,
  publishable_flag TINYINT NOT NULL DEFAULT 0,
  visibility_scope TINYINT NOT NULL DEFAULT 2,
  consent_version VARCHAR(32) DEFAULT NULL,
  current_consent_id BIGINT UNSIGNED DEFAULT NULL,
  current_snapshot_id BIGINT UNSIGNED DEFAULT NULL,
  control_status TINYINT NOT NULL DEFAULT 1 COMMENT '1 valid, 0 frozen',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_candidate_control (candidate_id),
  KEY idx_publishable_scope (publishable_flag, visibility_scope),
  CONSTRAINT fk_control_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE job_post (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  company_id BIGINT UNSIGNED NOT NULL,
  external_job_id VARCHAR(64) DEFAULT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  title VARCHAR(150) NOT NULL,
  category_code VARCHAR(64) DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  salary_min INT DEFAULT NULL,
  salary_max INT DEFAULT NULL,
  job_desc TEXT NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  audit_status TINYINT NOT NULL DEFAULT 1,
  sync_version INT NOT NULL DEFAULT 1,
  published_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_source_external_job (source_type, external_job_id),
  KEY idx_company_status (company_id, status),
  KEY idx_city_status (city_code, status),
  KEY idx_audit_status (audit_status),
  CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES company(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE job_application (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  job_id BIGINT UNSIGNED NOT NULL,
  candidate_id BIGINT UNSIGNED NOT NULL,
  resume_id BIGINT UNSIGNED DEFAULT NULL,
  source_type TINYINT NOT NULL DEFAULT 1,
  external_application_id VARCHAR(64) DEFAULT NULL,
  application_status TINYINT NOT NULL DEFAULT 0,
  apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_job_candidate_once (job_id, candidate_id),
  UNIQUE KEY uk_source_external_apply (source_type, external_application_id),
  KEY idx_candidate_application_status (candidate_id, application_status),
  KEY idx_job_status (job_id, application_status),
  CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES job_post(id),
  CONSTRAINT fk_application_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id),
  CONSTRAINT fk_application_resume FOREIGN KEY (resume_id) REFERENCES candidate_resume(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
