INSERT INTO admin_user (username, display_name, password_hash, role_code, status)
VALUES
  ('operator', '本地开发运营账号', '$2b$12$5G4kZ1x4dOar/VtPyL38UuxDYUreMg03NyJG11tnsQvlMZTSZhP2.', 'operator', 1)
AS new
ON DUPLICATE KEY UPDATE
  display_name = new.display_name,
  password_hash = new.password_hash,
  role_code = new.role_code,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;
