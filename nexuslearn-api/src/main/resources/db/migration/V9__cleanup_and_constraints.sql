ALTER TABLE chat_messages DROP COLUMN file_url;

ALTER TABLE assignment_submissions
    ADD CONSTRAINT unique_assignment_user_submission UNIQUE (assignment_id, user_id);