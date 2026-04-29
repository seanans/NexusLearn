package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(UUID assignmentId, UUID userId);

    List<AssignmentSubmission> findByAssignmentId(UUID assignmentId);

    @Query("SELECT s FROM AssignmentSubmission s " +
            "JOIN FETCH s.assignment a " +
            "JOIN FETCH a.module m " +
            "JOIN FETCH m.course c " +
            "WHERE s.id = :submissionId")
    Optional<AssignmentSubmission> findByIdWithCourseContext(@Param("submissionId") UUID submissionId);
}