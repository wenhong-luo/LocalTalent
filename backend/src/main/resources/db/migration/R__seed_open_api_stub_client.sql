INSERT INTO open_client (client_code, client_secret_hash, source_system, api_scope_json, status)
VALUES (
  'localtalent_stub',
  '97911de7ae1a8392865d8a6ce438d39b59236664f2985d0e84fd4be47be5779b',
  'stub_partner',
  JSON_ARRAY(
    'open.jobs.sync',
    'open.applications.sync',
    'open.consents.callback',
    'open.candidates.publishable_sync',
    'open.mappings.query'
  ),
  1
)
AS new
ON DUPLICATE KEY UPDATE
  client_secret_hash = new.client_secret_hash,
  source_system = new.source_system,
  api_scope_json = new.api_scope_json,
  status = new.status,
  updated_at = CURRENT_TIMESTAMP;
