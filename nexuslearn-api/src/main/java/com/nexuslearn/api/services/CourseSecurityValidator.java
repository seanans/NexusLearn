package com.nexuslearn.api.services;

import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.repositories.AssignmentRepository;
import com.nexuslearn.api.repositories.AssignmentSubmissionRepository;
import com.nexuslearn.api.repositories.CourseMemberRepository;
import com.nexuslearn.api.repositories.LessonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseSecurityValidator {

    private final CourseMemberRepository courseMemberRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public void validateAccess(UUID courseId, User user, boolean requireElevatedPrivileges) {
        CourseMemberId courseMemberId = new CourseMemberId(user.getId(), courseId);

        CourseMember courseMember = courseMemberRepository.findById(courseMemberId)
                .orElseThrow(() -> new AppException("Access Denied: You are not a member of this course", HttpStatus.FORBIDDEN));

        if (requireElevatedPrivileges) {
            CourseRole courseRole = courseMember.getRole();
            if (courseRole != CourseRole.TEACHER && courseRole != CourseRole.ASSISTANT) {
                throw new AppException("Access Denied: Insufficient privileges", HttpStatus.FORBIDDEN);
            }
        }
    }

    public CourseRole getUserRoleInCourse(UUID courseId, User user) {
        return courseMemberRepository.getRoleById(courseId, user.getId())
                .orElseThrow(() -> new AppException("You are not a member of this course", HttpStatus.FORBIDDEN));
    }

    public void validateAttachmentUpload(UUID entityId, EntityType entityType, User user) {
        switch (entityType) {
            case LESSON:
                Lesson lesson = lessonRepository.findById(entityId)
                        .orElseThrow(() -> new AppException("Lesson not found", HttpStatus.NOT_FOUND));
                validateAccess(lesson.getModule().getCourse().getId(), user, true);
                break;

            case ASSIGNMENT:
                Assignment assignment = assignmentRepository.findById(entityId)
                        .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));
                validateAccess(assignment.getModule().getCourse().getId(), user, true);
                break;

            case SUBMISSION:
                AssignmentSubmission submission = submissionRepository.findById(entityId)
                        .orElseThrow(() -> new AppException("Submission not found", HttpStatus.NOT_FOUND));

                if (!submission.getUser().getId().equals(user.getId())) {
                    throw new AppException("Access Denied: You can only attach files to your own submission", HttpStatus.FORBIDDEN);
                }
                break;

            case MESSAGE:
                // TODO: Chat validation
                break;

            default:
                throw new AppException("Invalid entity type", HttpStatus.BAD_REQUEST);
        }
    }

    public void validateAttachmentView(UUID entityId, EntityType entityType, User user) {
        switch (entityType) {
            case LESSON:
                Lesson lesson = lessonRepository.findById(entityId)
                        .orElseThrow(() -> new AppException("Lesson not found", HttpStatus.NOT_FOUND));
                validateAccess(lesson.getModule().getCourse().getId(), user, false);
                break;

            case ASSIGNMENT:
                Assignment assignment = assignmentRepository.findById(entityId)
                        .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));
                validateAccess(assignment.getModule().getCourse().getId(), user, false);
                break;

            case SUBMISSION:
                AssignmentSubmission submission = submissionRepository.findById(entityId)
                        .orElseThrow(() -> new AppException("Submission not found", HttpStatus.NOT_FOUND));

                if (submission.getUser().getId().equals(user.getId())) {
                    return;
                }

                validateAccess(submission.getAssignment().getModule().getCourse().getId(), user, true);
                break;

            case MESSAGE:
                // TODO: Chat validation
                break;

            default:
                throw new AppException("Invalid entity type", HttpStatus.BAD_REQUEST);
        }
    }
}