CREATE TABLE user_session (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES user_account(id) ON DELETE CASCADE,
    token_hash  VARCHAR(200) NOT NULL,
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_user_session_token_hash UNIQUE (token_hash)
);

CREATE INDEX ix_user_session_user_id ON user_session (user_id);

