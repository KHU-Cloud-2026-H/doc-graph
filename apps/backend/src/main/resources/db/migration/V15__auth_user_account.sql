CREATE TABLE user_account (
    id             BIGSERIAL PRIMARY KEY,
    email          VARCHAR(320) NOT NULL,
    name           VARCHAR(200) NOT NULL,
    avatar_url     VARCHAR(1000),
    notion_user_id VARCHAR(100) NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_user_account_email UNIQUE (email),
    CONSTRAINT uk_user_account_notion_user_id UNIQUE (notion_user_id),
    CONSTRAINT ck_user_account_email CHECK (email <> ''),
    CONSTRAINT ck_user_account_name CHECK (name <> ''),
    CONSTRAINT ck_user_account_notion_user_id CHECK (notion_user_id <> '')
);

