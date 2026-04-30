ALTER TABLE company
  ADD COLUMN nature_code VARCHAR(64) DEFAULT NULL AFTER industry_code;

CREATE INDEX idx_company_portal_search
  ON company (auth_status, city_code, industry_code, nature_code, scale_code, updated_at);
