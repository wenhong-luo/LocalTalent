CREATE TABLE api_idempotency_record (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  api_code VARCHAR(128) NOT NULL,
  principal_type VARCHAR(32) NOT NULL,
  principal_id BIGINT UNSIGNED NOT NULL,
  idempotency_key VARCHAR(128) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  response_json JSON NOT NULL,
  resource_type VARCHAR(64) DEFAULT NULL,
  resource_id BIGINT UNSIGNED DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_api_principal_key (api_code, principal_type, principal_id, idempotency_key),
  KEY idx_resource (resource_type, resource_id),
  KEY idx_api_created (api_code, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
