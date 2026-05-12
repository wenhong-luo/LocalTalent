CREATE TABLE company_style_image (
    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    company_id BIGINT UNSIGNED NOT NULL,
    file_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    object_key VARCHAR(500) NOT NULL,
    display_order INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 1,
    review_status TINYINT NOT NULL DEFAULT 0,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_company_style_image_company
        FOREIGN KEY (company_id) REFERENCES company (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE INDEX idx_company_style_active_order
    ON company_style_image (company_id, status, display_order, id);

CREATE INDEX idx_company_style_review
    ON company_style_image (review_status, status);

CREATE INDEX idx_company_style_sha
    ON company_style_image (sha256);
