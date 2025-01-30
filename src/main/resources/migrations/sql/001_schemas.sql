CREATE SCHEMA IF NOT EXISTS audiodemo;

CREATE TABLE IF NOT EXISTS audiodemo.users (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at timestamp with time zone DEFAULT NOW() NOT NULL
);

CREATE TABLE IF NOT EXISTS audiodemo.phrases (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at timestamp with time zone DEFAULT NOW() NOT NULL
);

CREATE TABLE IF NOT EXISTS audiodemo.user_phrase_files (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id int NOT NULL,
    phrase_id int NOT NULL,
    file_name VARCHAR(255),
    created_at timestamp with time zone DEFAULT NOW() NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT user_phrases_users_fk FOREIGN KEY (user_id) REFERENCES audiodemo.users(id),
    CONSTRAINT user_phrases_phrases_fk FOREIGN KEY (phrase_id) REFERENCES audiodemo.phrases(id)
);

CREATE INDEX IF NOT EXISTS idx_user_phrase_deleted_filtered ON audiodemo.user_phrase_files (user_id, phrase_id) WHERE deleted_at IS NOT NULL;
