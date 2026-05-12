ALTER TABLE company
  ADD COLUMN registered_capital_amount VARCHAR(32) DEFAULT NULL AFTER certification_material_summary_json,
  ADD COLUMN registered_capital_unit VARCHAR(16) DEFAULT NULL AFTER registered_capital_amount,
  ADD COLUMN website_url VARCHAR(255) DEFAULT NULL AFTER registered_capital_unit,
  ADD COLUMN benefit_codes_json JSON DEFAULT NULL AFTER website_url,
  ADD COLUMN contact_name VARCHAR(100) DEFAULT NULL AFTER benefit_codes_json,
  ADD COLUMN contact_mobile VARCHAR(32) DEFAULT NULL AFTER contact_name,
  ADD COLUMN contact_mobile_hidden TINYINT NOT NULL DEFAULT 1 AFTER contact_mobile,
  ADD COLUMN contact_wechat VARCHAR(64) DEFAULT NULL AFTER contact_mobile_hidden,
  ADD COLUMN contact_wechat_same_mobile TINYINT NOT NULL DEFAULT 0 AFTER contact_wechat,
  ADD COLUMN contact_phone VARCHAR(64) DEFAULT NULL AFTER contact_wechat_same_mobile,
  ADD COLUMN contact_email VARCHAR(128) DEFAULT NULL AFTER contact_phone,
  ADD COLUMN contact_qq VARCHAR(32) DEFAULT NULL AFTER contact_email;
