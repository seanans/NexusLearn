package com.nexuslearn.api.repositories;

import com.nexuslearn.api.dtos.AssignmentSummaryProjection;
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

    List<AssignmentSummaryProjection> findByModuleIdOrderByOrderIndexAsc(UUID moduleId);

    @Query("SELECT COALESCE(MAX(a.orderIndex), 0) FROM Assignment a WHERE a.module.id = :moduleId")
    Integer findMaxOrderIndexByModuleId(@Param("moduleId") UUID moduleId);

    @Query("SELECT a FROM Assignment a " +
            "JOIN FETCH a.module m " +
            "JOIN FETCH m.course c " +
            "WHERE a.id = :assignmentId")
    Optional<Assignment> findByIdWithCourseContext(@Param("assignmentId") UUID assignmentId);
}
