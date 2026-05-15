ALTER TABLE portal_home_operation_slot
  ADD COLUMN image_object_key VARCHAR(512) NULL AFTER image_url,
  ADD COLUMN image_file_name VARCHAR(255) NULL AFTER image_object_key,
  ADD COLUMN image_content_type VARCHAR(120) NULL AFTER image_file_name,
  ADD COLUMN image_size_bytes BIGINT NULL AFTER image_content_type,
  ADD COLUMN image_sha256 CHAR(64) NULL AFTER image_size_bytes,
  ADD COLUMN image_uploaded_at DATETIME NULL AFTER image_sha256,
  ADD KEY idx_home_slot_image_status (status, image_uploaded_at);
