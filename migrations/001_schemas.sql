CREATE SCHEMA audiodemo AUTHORIZATION audiouser;

CREATE TABLE audiodemo.users (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at timestamp with time zone DEFAULT NOW() NOT NULL
);

CREATE TABLE audiodemo.phrases (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    created_at timestamp with time zone DEFAULT NOW() NOT NULL
);

CREATE TABLE audiodemo.user_phrase_files (
    id int GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id int NOT NULL,
    phrase_id int NOT NULL,
    file_name VARCHAR(255),
    created_at timestamp with time zone DEFAULT NOW() NOT NULL,
    CONSTRAINT user_phrases_users_fk FOREIGN KEY (user_id) REFERENCES audiodemo.users(id),
    CONSTRAINT user_phrases_phrases_fk FOREIGN KEY (phrase_id) REFERENCES audiodemo.phrases(id)
);
