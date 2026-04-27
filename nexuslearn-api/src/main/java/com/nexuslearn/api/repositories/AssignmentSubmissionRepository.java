package com.nexuslearn.api.repositories;

import com.nexuslearn.api.models.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission> findByAssignmentIdAndUserId(UUID assignmentId, UUID userId);

    List<AssignmentSubmission> findByAssignmentId(UUID assignmentId);
}