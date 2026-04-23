package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
}