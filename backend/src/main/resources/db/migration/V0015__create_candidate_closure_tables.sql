CREATE TABLE candidate_job_favorite (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  job_id BIGINT UNSIGNED NOT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 0 cancelled',
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_candidate_job_favorite (candidate_id, job_id),
  KEY idx_candidate_favorite_status (candidate_id, status, updated_at),
  KEY idx_favorite_job (job_id, status),
  CONSTRAINT fk_favorite_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id),
  CONSTRAINT fk_favorite_job FOREIGN KEY (job_id) REFERENCES job_post(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_search_subscription (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  subscription_name VARCHAR(120) NOT NULL,
  keyword VARCHAR(120) DEFAULT NULL,
  city_code VARCHAR(32) DEFAULT NULL,
  category_code VARCHAR(64) DEFAULT NULL,
  salary_min INT DEFAULT NULL,
  salary_max INT DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1 COMMENT '1 active, 0 cancelled',
  last_triggered_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_candidate_subscription_status (candidate_id, status, updated_at),
  KEY idx_subscription_filter (city_code, category_code, status),
  CONSTRAINT fk_subscription_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE candidate_notification (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  candidate_id BIGINT UNSIGNED NOT NULL,
  notification_type VARCHAR(64) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content_summary VARCHAR(500) DEFAULT NULL,
  biz_type VARCHAR(64) DEFAULT NULL,
  biz_id BIGINT UNSIGNED DEFAULT NULL,
  read_status TINYINT NOT NULL DEFAULT 0 COMMENT '0 unread, 1 read',
  read_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_candidate_notification_read (candidate_id, read_status, created_at),
  KEY idx_candidate_notification_biz (candidate_id, biz_type, biz_id),
  CONSTRAINT fk_notification_candidate FOREIGN KEY (candidate_id) REFERENCES candidate_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
