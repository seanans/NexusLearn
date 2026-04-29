-- V4__enforce_ordering_constraints.sql
ALTER TABLE modules ADD CONSTRAINT unique_course_module_order UNIQUE (course_id, order_index);
ALTER TABLE lessons ADD CONSTRAINT unique_module_lesson_order UNIQUE (module_id, order_index);
ALTER TABLE assignments ADD CONSTRAINT unique_module_assignment_order UNIQUE (module_id, order_index);