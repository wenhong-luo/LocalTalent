CREATE TABLE integration_mapping (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  local_biz_type VARCHAR(32) NOT NULL,
  local_id BIGINT UNSIGNED NOT NULL,
  source_system VARCHAR(32) NOT NULL,
  external_id VARCHAR(64) NOT NULL,
  version INT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_mapping (local_biz_type, local_id, source_system),
  UNIQUE KEY uk_external_mapping (source_system, external_id),
  KEY idx_source_system (source_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE integration_sync_task (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  source_system VARCHAR(32) NOT NULL,
  target_system VARCHAR(32) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  payload_json JSON DEFAULT NULL,
  sync_status TINYINT NOT NULL DEFAULT 0,
  retry_count INT NOT NULL DEFAULT 0,
  sync_version INT NOT NULL DEFAULT 1,
  last_sync_time DATETIME DEFAULT NULL,
  next_retry_time DATETIME DEFAULT NULL,
  error_msg VARCHAR(500) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_sync_status_time (sync_status, next_retry_time),
  KEY idx_biz (biz_type, biz_id),
  KEY idx_source_target (source_system, target_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE open_api_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  source_system VARCHAR(32) NOT NULL,
  api_code VARCHAR(64) NOT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  request_uri VARCHAR(255) NOT NULL,
  request_method VARCHAR(16) NOT NULL,
  biz_type VARCHAR(32) DEFAULT NULL,
  biz_id BIGINT UNSIGNED DEFAULT NULL,
  http_status INT NOT NULL DEFAULT 200,
  success_flag TINYINT NOT NULL DEFAULT 1,
  request_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  response_time DATETIME DEFAULT NULL,
  error_msg VARCHAR(500) DEFAULT NULL,
  KEY idx_source_api_time (source_system, api_code, request_time),
  KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audit_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT UNSIGNED NOT NULL,
  operator_role VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  action_type VARCHAR(64) NOT NULL,
  before_json JSON DEFAULT NULL,
  after_json JSON DEFAULT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_biz_action_time (biz_type, biz_id, created_at),
  KEY idx_operator_time (operator_id, created_at),
  KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE field_access_log (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT UNSIGNED NOT NULL,
  operator_role VARCHAR(64) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  biz_id BIGINT UNSIGNED NOT NULL,
  field_name VARCHAR(128) NOT NULL,
  access_type VARCHAR(32) NOT NULL,
  trace_id VARCHAR(64) DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_operator_field_time (operator_id, field_name, created_at),
  KEY idx_biz_field_time (biz_type, biz_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE export_apply (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  apply_user_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  scope_json JSON NOT NULL,
  reason VARCHAR(500) DEFAULT NULL,
  approve_status TINYINT NOT NULL DEFAULT 0,
  approve_user_id BIGINT UNSIGNED DEFAULT NULL,
  approve_time DATETIME DEFAULT NULL,
  download_url VARCHAR(500) DEFAULT NULL,
  expire_time DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_apply_status_time (approve_status, created_at),
  KEY idx_apply_user_time (apply_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
