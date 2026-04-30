package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.SubmissionCreateRequest;
import com.nexuslearn.api.dtos.SubmissionGradeRequest;
import com.nexuslearn.api.dtos.SubmissionResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Assignment;
import com.nexuslearn.api.models.AssignmentSubmission;
import com.nexuslearn.api.models.CourseRole;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.AssignmentRepository;
import com.nexuslearn.api.repositories.AssignmentSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionService {
    private final AssignmentSubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseSecurityValidator securityValidator;

    @Transactional
    public SubmissionResponse submitAssignment(UUID assignmentId, SubmissionCreateRequest request, User user) {
        Assignment assignment = assignmentRepository.findById(assignmentId).orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        CourseRole userRole = securityValidator.getUserRoleInCourse(assignment.getModule().getCourse().getId(), user);
        if (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) {
            throw new AppException("Teachers and Assistants cannot submit assignments", HttpStatus.BAD_REQUEST);
        }

        AssignmentSubmission submission = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId()).orElseGet(() -> AssignmentSubmission.builder().assignment(assignment).user(user).build());

        submission.setSubmissionText(request.getSubmissionText());

        submission.setScore(null);
        submission.setFeedback(null);

        submission = submissionRepository.save(submission);
        return mapToResponse(submission);
    }

    @Transactional
    public SubmissionResponse gradeSubmission(UUID submissionId, SubmissionGradeRequest request, User user) {
        AssignmentSubmission submission = submissionRepository.findByIdWithCourseContext(submissionId).orElseThrow(() -> new AppException("Submission not found", HttpStatus.NOT_FOUND));

        Assignment assignment = submission.getAssignment();
        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);

        if (request.getScore() > assignment.getMaxScore()) {
            throw new AppException("Score exceeds maximum", HttpStatus.BAD_REQUEST);
        }

        submission.setScore(request.getScore());
        submission.setFeedback(request.getFeedback());

        submission = submissionRepository.save(submission);
        return mapToResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResponse> getSubmissionsForAssignment(UUID assignmentId, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId).orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        CourseRole userRole = securityValidator.getUserRoleInCourse(assignment.getModule().getCourse().getId(), user);

        if (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) {
            return submissionRepository.findByAssignmentId(assignmentId).stream().map(this::mapToResponse).collect(Collectors.toList());
        } else {
            SubmissionResponse singleResponse = submissionRepository.findByAssignmentIdAndUserId(assignmentId, user.getId()).map(this::mapToResponse).orElseThrow(() -> new AppException("No submission found for this user", HttpStatus.NOT_FOUND));

            return List.of(singleResponse);
        }
    }

    private SubmissionResponse mapToResponse(AssignmentSubmission submission) {
        LocalDateTime evaluationTime = submission.getUpdatedAt() != null ? submission.getUpdatedAt() : LocalDateTime.now();
        boolean isLate = evaluationTime.isAfter(submission.getAssignment().getDueDate());

        return SubmissionResponse.builder().id(submission.getId()).assignmentId(submission.getAssignment().getId()).userId(submission.getUser().getId()).submissionText(submission.getSubmissionText()).score(submission.getScore()).feedback(submission.getFeedback()).submittedAt(submission.getUpdatedAt()).isLate(isLate).build();
    }
}
