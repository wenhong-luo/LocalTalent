ALTER TABLE job_post
    ADD COLUMN job_nature_code VARCHAR(32) NULL AFTER title,
    ADD COLUMN category_name VARCHAR(100) NULL AFTER category_code,
    ADD COLUMN experience_code VARCHAR(32) NULL AFTER category_name,
    ADD COLUMN education_code VARCHAR(32) NULL AFTER experience_code,
    ADD COLUMN recruit_count INT NULL AFTER education_code,
    ADD COLUMN salary_negotiable TINYINT NOT NULL DEFAULT 0 AFTER salary_max,
    ADD COLUMN work_region_path VARCHAR(160) NULL AFTER city_code,
    ADD COLUMN address VARCHAR(255) NULL AFTER work_region_path,
    ADD COLUMN welfare_codes JSON NULL AFTER job_desc,
    ADD COLUMN department_name VARCHAR(100) NULL AFTER welfare_codes,
    ADD COLUMN age_min INT NULL AFTER department_name,
    ADD COLUMN age_max INT NULL AFTER age_min,
    ADD COLUMN age_unlimited TINYINT NOT NULL DEFAULT 0 AFTER age_max,
    ADD COLUMN recruitment_time_code VARCHAR(32) NULL AFTER age_unlimited,
    ADD COLUMN contact_mode VARCHAR(32) NULL AFTER recruitment_time_code,
    ADD COLUMN contact_name VARCHAR(80) NULL AFTER contact_mode,
    ADD COLUMN contact_mobile VARCHAR(32) NULL AFTER contact_name,
    ADD COLUMN contact_phone VARCHAR(64) NULL AFTER contact_mobile,
    ADD COLUMN contact_email VARCHAR(120) NULL AFTER contact_phone,
    ADD COLUMN contact_wechat VARCHAR(64) NULL AFTER contact_email,
    ADD COLUMN contact_hidden TINYINT NOT NULL DEFAULT 1 AFTER contact_wechat,
    ADD COLUMN notify_enabled TINYINT NOT NULL DEFAULT 0 AFTER contact_hidden,
    ADD COLUMN resume_subscription_enabled TINYINT NOT NULL DEFAULT 0 AFTER notify_enabled;

CREATE INDEX idx_job_post_publish_profile
    ON job_post (job_nature_code, experience_code, education_code);
