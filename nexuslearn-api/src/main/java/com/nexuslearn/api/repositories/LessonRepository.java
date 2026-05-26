package com.nexuslearn.api.repositories;

import com.nexuslearn.api.dtos.LessonSummaryProjection;
import com.nexuslearn.api.models.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<LessonSummaryProjection> findByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    @Query("SELECT COALESCE(MAX(l.orderIndex), 0) FROM Lesson l WHERE l.module.id = :moduleId")
    Integer findMaxOrderIndexByModuleId(@Param("moduleId") UUID moduleId);

    @Query("""
                 SELECT l FROM Lesson l\s
                 WHERE l.module.id = :moduleId\s
                 AND l.isPublished = true\s
                 AND l.module.isPublished = true\s
                 AND (l.availableFrom IS NULL OR l.availableFrom <= CURRENT_TIMESTAMP)
                 ORDER BY l.orderIndex ASC
            \s""")
    List<LessonSummaryProjection> findVisibleLessonsForStudent(@Param("moduleId") UUID moduleId);

    // bulk fetch for syllabus
    // for Teachers/Assistants
    List<Lesson> findByModuleIdIn(List<UUID> moduleIds);

    // for Students
    @Query("""
                SELECT l FROM Lesson l\s
                WHERE l.module.id IN :moduleIds\s
                AND l.isPublished = true\s
                AND l.module.isPublished = true\s
                AND (l.availableFrom IS NULL OR l.availableFrom <= CURRENT_TIMESTAMP)
           \s""")
    List<Lesson> findVisibleByModuleIdIn(@Param("moduleIds") List<UUID> moduleIds);


    // detail fetch
    // for Teachers/Assistants
    Optional<LessonSummaryProjection> findProjectedByIdAndModule_Course_Id(UUID id, UUID courseId);

    // for Students
    @Query("""
                SELECT l FROM Lesson l\s
                WHERE l.id = :id\s
                AND l.module.course.id = :courseId\s
                AND l.isPublished = true\s
                AND l.module.isPublished = true\s
                AND (l.availableFrom IS NULL OR l.availableFrom <= CURRENT_TIMESTAMP)
           \s""")
    Optional<LessonSummaryProjection> findVisibleProjectedByIdAndCourseId(@Param("id") UUID id, @Param("courseId") UUID courseId);
}
