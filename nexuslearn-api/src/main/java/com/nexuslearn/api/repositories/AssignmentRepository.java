package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    @Query("SELECT COALESCE(MAX(a.orderIndex), 0) FROM Assignment a WHERE a.module.id = :moduleId")
    Integer findMaxOrderIndexByModuleId(@Param("moduleId") UUID moduleId);

    @Query("""
                  SELECT a FROM Assignment a\s
                  WHERE a.module.id = :moduleId\s
                  AND a.isPublished = true\s
                  AND a.module.isPublished = true\s
                  AND (a.availableFrom IS NULL OR a.availableFrom <= CURRENT_TIMESTAMP)
                  ORDER BY a.orderIndex ASC
            \s""")
    List<Assignment> findVisibleAssignmentsForStudent(@Param("moduleId") UUID moduleId);

    // bulk fetch for syllabus
    List<Assignment> findByModuleIdIn(List<UUID> moduleIds);

    @Query("""
                  SELECT a FROM Assignment a\s
                  WHERE a.module.id IN :moduleIds\s
                  AND a.isPublished = true\s
                  AND a.module.isPublished = true\s
                  AND (a.availableFrom IS NULL OR a.availableFrom <= CURRENT_TIMESTAMP)
            \s""")
    List<Assignment> findVisibleByModuleIdIn(@Param("moduleIds") List<UUID> moduleIds);

    Optional<Assignment> findByIdAndModule_Course_Id(UUID id, UUID courseId);

    @Query("""
                  SELECT a FROM Assignment a\s
                  WHERE a.id = :id\s
                  AND a.module.course.id = :courseId\s
                  AND a.isPublished = true\s
                  AND a.module.isPublished = true\s
                  AND (a.availableFrom IS NULL OR a.availableFrom <= CURRENT_TIMESTAMP)
            \s""")
    Optional<Assignment> findVisibleByIdAndCourseId(@Param("id") UUID id, @Param("courseId") UUID courseId);

    @Query("SELECT a FROM Assignment a " + "JOIN FETCH a.module m " + "JOIN FETCH m.course c " + "WHERE a.id = :assignmentId")
    Optional<Assignment> findByIdWithCourseContext(@Param("assignmentId") UUID assignmentId);
}