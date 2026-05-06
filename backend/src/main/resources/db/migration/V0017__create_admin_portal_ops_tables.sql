CREATE TABLE portal_recommendation (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  slot_code VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  title_override VARCHAR(200) DEFAULT NULL,
  summary_override VARCHAR(500) DEFAULT NULL,
  display_order INT NOT NULL DEFAULT 100,
  status TINYINT NOT NULL DEFAULT 1,
  start_time DATETIME DEFAULT NULL,
  end_time DATETIME DEFAULT NULL,
  operator_id BIGINT UNSIGNED DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_slot_status_order (slot_code, status, display_order, updated_at),
  KEY idx_target (target_type, target_id),
  KEY idx_status_time (status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE risk_review (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  risk_type VARCHAR(64) NOT NULL,
  target_type VARCHAR(32) NOT NULL,
  target_id BIGINT UNSIGNED NOT NULL,
  severity VARCHAR(32) NOT NULL DEFAULT 'medium',
  status TINYINT NOT NULL DEFAULT 0,
  title VARCHAR(200) NOT NULL,
  summary VARCHAR(500) DEFAULT NULL,
  decision VARCHAR(500) DEFAULT NULL,
  handler_id BIGINT UNSIGNED DEFAULT NULL,
  handled_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_status_severity_time (status, severity, created_at),
  KEY idx_target (target_type, target_id),
  KEY idx_handler_time (handler_id, handled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
