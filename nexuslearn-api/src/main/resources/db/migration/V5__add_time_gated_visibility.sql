-- Add visibility and scheduling columns to lessons
ALTER TABLE lessons
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN available_from TIMESTAMP;

-- Add visibility and scheduling columns to assignments
ALTER TABLE assignments
    ADD COLUMN is_published BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN available_from TIMESTAMP;