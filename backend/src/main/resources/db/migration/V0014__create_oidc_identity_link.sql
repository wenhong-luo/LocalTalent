CREATE TABLE auth_oidc_identity_link (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  issuer VARCHAR(255) NOT NULL,
  subject_sha256 CHAR(64) NOT NULL,
  identity_type VARCHAR(32) NOT NULL COMMENT 'candidate/company/operator',
  user_id BIGINT UNSIGNED NOT NULL,
  company_id BIGINT UNSIGNED DEFAULT NULL,
  email_hash CHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  first_linked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  last_login_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_oidc_issuer_subject (issuer, subject_sha256),
  KEY idx_oidc_identity (identity_type, user_id),
  KEY idx_oidc_status_login (status, last_login_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
