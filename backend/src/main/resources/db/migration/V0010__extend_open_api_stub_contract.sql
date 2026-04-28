CREATE TABLE open_api_nonce_record (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  client_code VARCHAR(64) NOT NULL,
  nonce VARCHAR(128) NOT NULL,
  timestamp_at DATETIME NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_client_nonce (client_code, nonce),
  KEY idx_nonce_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE open_api_log
  ADD COLUMN client_code VARCHAR(64) DEFAULT NULL AFTER source_system,
  ADD COLUMN request_hash VARCHAR(64) DEFAULT NULL AFTER request_method,
  ADD COLUMN idempotency_key VARCHAR(128) DEFAULT NULL AFTER request_hash,
  ADD COLUMN duration_ms BIGINT DEFAULT NULL AFTER response_time,
  ADD COLUMN request_summary_json JSON DEFAULT NULL AFTER duration_ms,
  ADD COLUMN response_summary_json JSON DEFAULT NULL AFTER request_summary_json,
  ADD KEY idx_client_api_time (client_code, api_code, request_time),
  ADD KEY idx_request_hash (request_hash);

ALTER TABLE integration_sync_task
  ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL AFTER id,
  ADD COLUMN api_code VARCHAR(128) DEFAULT NULL AFTER trace_id,
  ADD COLUMN open_api_log_id BIGINT UNSIGNED DEFAULT NULL AFTER api_code,
  ADD COLUMN max_retry_count INT NOT NULL DEFAULT 4 AFTER retry_count,
  ADD KEY idx_open_log (open_api_log_id),
  ADD KEY idx_trace_id (trace_id),
  ADD KEY idx_api_status_time (api_code, sync_status, next_retry_time);
