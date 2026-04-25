package com.nexuslearn.api.repositories;

import com.nexuslearn.api.dtos.CourseDashboardProjection;
import com.nexuslearn.api.models.Course;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    @Query("SELECT c.id as id, c.title as title, c.description as description, " +
            "c.lastActivityMessage as lastActivityMessage, c.lastActivityAt as lastActivityAt, " + // NEW
            "t.firstName as teacherFirstName, t.lastName as teacherLastName " +
            "FROM Course c " +
            "JOIN CourseMember myCm ON c = myCm.course " +
            "LEFT JOIN CourseMember teacherCm ON c = teacherCm.course AND teacherCm.role = 'TEACHER' " +
            "LEFT JOIN User t ON teacherCm.user = t " +
            "WHERE myCm.user.id = :userId")
    Slice<CourseDashboardProjection> findDashboardCourses(@Param("userId") UUID userId, Pageable pageable);
}