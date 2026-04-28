ALTER TABLE candidate_publish_snapshot
    ADD COLUMN city_code VARCHAR(32)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.city_code'))) STORED,
    ADD COLUMN category_code VARCHAR(64)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.category_code'))) STORED,
    ADD COLUMN display_name_masked VARCHAR(128)
        GENERATED ALWAYS AS (JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.display_name_masked'))) STORED,
    ADD COLUMN experience_years INT
        GENERATED ALWAYS AS (
            CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(snapshot_json, '$.experience_years')), 'null') AS SIGNED)
        ) STORED,
    ADD KEY idx_portal_snapshot_visible_time (
        publishable_flag,
        consent_status,
        status,
        visibility_scope,
        updated_at,
        id
    ),
    ADD KEY idx_portal_snapshot_filter_time (
        publishable_flag,
        consent_status,
        status,
        visibility_scope,
        city_code,
        category_code,
        updated_at,
        id
    );
