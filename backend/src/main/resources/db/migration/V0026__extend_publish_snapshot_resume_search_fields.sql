ALTER TABLE candidate_publish_snapshot
    ADD COLUMN education_code VARCHAR(64)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.education_code'))) STORED,
    ADD COLUMN gender_code VARCHAR(32)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.gender'))) STORED,
    ADD COLUMN industry_code VARCHAR(64)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.industry_code'))) STORED,
    ADD COLUMN major_name VARCHAR(128)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.major_name'))) STORED,
    ADD COLUMN work_nature_code VARCHAR(64)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.work_nature'))) STORED,
    ADD COLUMN expected_salary_code VARCHAR(64)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.expected_salary_code'))) STORED,
    ADD COLUMN resume_tags_text VARCHAR(256)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.resume_tags_text'))) STORED,
    ADD KEY idx_resume_search_base (
        publishable_flag,
        consent_status,
        status,
        visibility_scope,
        city_code,
        category_code,
        education_code,
        experience_years,
        updated_at,
        id
    ),
    ADD KEY idx_resume_search_dropdown (
        publishable_flag,
        consent_status,
        status,
        visibility_scope,
        gender_code,
        industry_code,
        work_nature_code,
        expected_salary_code,
        updated_at,
        id
    );
