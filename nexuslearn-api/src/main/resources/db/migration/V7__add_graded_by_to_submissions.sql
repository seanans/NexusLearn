ALTER TABLE assignment_submissions
    ADD COLUMN graded_by_id UUID;

ALTER TABLE assignment_submissions
    ADD CONSTRAINT fk_submissions_graded_by
        FOREIGN KEY (graded_by_id) REFERENCES users(id)
            ON DELETE SET NULL;