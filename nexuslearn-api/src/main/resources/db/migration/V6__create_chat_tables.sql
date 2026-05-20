-- V6__create_chat_tables.sql
CREATE TABLE chat_messages
(
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    course_id   UUID REFERENCES courses (id) ON DELETE CASCADE,
    sender_id   UUID REFERENCES users (id) ON DELETE CASCADE,
    content     TEXT,
    file_url    VARCHAR(1024),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_chat_messages_course_id ON chat_messages(course_id);