CREATE TABLE sys_role (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_code VARCHAR(64) NOT NULL,
  role_name VARCHAR(128) NOT NULL,
  role_type VARCHAR(32) NOT NULL COMMENT 'portal/company/admin/open',
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_menu (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  menu_code VARCHAR(64) NOT NULL,
  menu_name VARCHAR(128) NOT NULL,
  parent_id BIGINT UNSIGNED DEFAULT NULL,
  menu_type VARCHAR(32) NOT NULL COMMENT 'menu/page/api/button',
  route_path VARCHAR(255) DEFAULT NULL,
  api_code VARCHAR(128) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_menu_code (menu_code),
  KEY idx_parent_sort (parent_id, sort_no),
  CONSTRAINT fk_menu_parent FOREIGN KEY (parent_id) REFERENCES sys_menu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_menu (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  menu_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_role_menu (role_id, menu_id),
  KEY idx_role_menu_menu (menu_id),
  CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id),
  CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user_role (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  user_type VARCHAR(32) NOT NULL COMMENT 'candidate/company_user/admin_user',
  user_id BIGINT UNSIGNED NOT NULL,
  role_id BIGINT UNSIGNED NOT NULL,
  UNIQUE KEY uk_user_role (user_type, user_id, role_id),
  KEY idx_role_user (role_id, user_type, user_id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_data_scope (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  scope_type VARCHAR(32) NOT NULL COMMENT 'self/company/job/session/aggregate/open_scope',
  scope_value_rule VARCHAR(255) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_role_biz_scope (role_id, biz_type, scope_type),
  CONSTRAINT fk_data_scope_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_role_field_policy (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  role_id BIGINT UNSIGNED NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  field_name VARCHAR(128) NOT NULL,
  policy_type VARCHAR(16) NOT NULL COMMENT 'ALLOW/MASK/DENY',
  mask_rule VARCHAR(64) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_role_field_policy (role_id, biz_type, field_name),
  CONSTRAINT fk_field_policy_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE open_client (
  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
  client_code VARCHAR(64) NOT NULL,
  client_secret_hash VARCHAR(255) NOT NULL,
  source_system VARCHAR(32) NOT NULL,
  api_scope_json JSON NOT NULL,
  status TINYINT NOT NULL DEFAULT 1,
  last_call_at DATETIME DEFAULT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_client_code (client_code),
  KEY idx_source_system (source_system)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
