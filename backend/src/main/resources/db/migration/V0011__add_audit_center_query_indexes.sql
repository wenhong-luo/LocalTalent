ALTER TABLE audit_log
  ADD KEY idx_audit_trace_time (trace_id, created_at);

ALTER TABLE field_access_log
  ADD KEY idx_field_access_trace_time (trace_id, created_at);

ALTER TABLE open_api_log
  ADD KEY idx_open_api_trace_time (trace_id, request_time);
