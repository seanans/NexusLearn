CREATE TABLE attachments (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             entity_id UUID NOT NULL,
                             entity_type VARCHAR(50) NOT NULL,
                             file_url VARCHAR(1024) NOT NULL,
                             file_name VARCHAR(255) NOT NULL,
                             file_type VARCHAR(50) NOT NULL,
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_attachments_entity ON attachments(entity_type, entity_id);

ALTER TABLE lessons DROP COLUMN video_url;