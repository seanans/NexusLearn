-- V10__add_submission_timestamps.sql
ALTER TABLE assignment_submissions
    ADD COLUMN submitted_at TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN graded_at TIMESTAMP WITHOUT TIME ZONE;
