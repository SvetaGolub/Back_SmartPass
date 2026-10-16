CREATE INDEX idx_expires ON dynamic_tokens(expires_at);
CREATE INDEX idx_user_token ON dynamic_tokens(user_id, token);