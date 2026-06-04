package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.*;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.AssignmentRepository;
import com.nexuslearn.api.repositories.AttachmentRepository;
import com.nexuslearn.api.repositories.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ModuleRepository moduleRepository;
    private final CourseSecurityValidator securityValidator;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    public UUID createAssignment(UUID moduleId, AssignmentCreateRequest request, User user) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(module.getCourse().getId(), user, true);

        Integer nextOrderIndex = assignmentRepository.findMaxOrderIndexByModuleId(moduleId) + 1;

        Assignment assignment = Assignment.builder()
                .module(module)
                .title(request.getTitle())
                .description(request.getDescription())
                .maxScore(request.getMaxScore())
                .dueDate(request.getDueDate())
                .orderIndex(nextOrderIndex)
                .isPublished(request.getIsPublished() != null ? request.getIsPublished() : false)
                .availableFrom(request.getAvailableFrom())
                .build();
        assignment = assignmentRepository.save(assignment);
        return assignment.getId();
    }

    @Transactional(readOnly = true)
    public List<AssignmentResponse> getAssignmentsByModule(UUID moduleId, User user) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        CourseRole userRole = securityValidator.getUserRoleInCourse(module.getCourse().getId(), user);
        if (userRole == CourseRole.STUDENT && !module.getIsPublished()) {
            throw new AppException("Access Denied: This module is unpublished", HttpStatus.FORBIDDEN);
        }

        List<Assignment> assignments;
        if (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) {
            assignments = assignmentRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        } else {
            assignments = assignmentRepository.findVisibleAssignmentsForStudent(moduleId);
        }

        return assignments.stream().map(a -> AssignmentResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .maxScore(a.getMaxScore())
                .dueDate(a.getDueDate())
                .orderIndex(a.getOrderIndex())
                .published(a.getIsPublished())
                .availableFrom(a.getAvailableFrom())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build()).collect(Collectors.toList());
    }

    @Transactional
    public void updateAssignment(UUID assignmentId, AssignmentUpdateRequest request, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId).orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setDueDate(request.getDueDate());
        assignment.setAvailableFrom(request.getAvailableFrom());
        assignment.setIsPublished(request.getIsPublished());

        assignmentRepository.save(assignment);

        if (request.getNewAttachments() != null && !request.getNewAttachments().isEmpty()) {
            for (PendingAttachmentDto stagedFile : request.getNewAttachments()) {
                Attachment newAttachment = new Attachment();
                newAttachment.setEntityId(assignment.getId());
                newAttachment.setEntityType(EntityType.ASSIGNMENT);
                newAttachment.setFileUrl(stagedFile.getFileUrl());
                newAttachment.setFileName(stagedFile.getFileName());
                newAttachment.setFileType(stagedFile.getFileType());

                attachmentRepository.save(newAttachment);
            }
        }
    }

    @Transactional
    public void updateAssignmentPublishStatus(UUID assignmentId, Boolean isPublished, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId).orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);

        assignment.setIsPublished(isPublished);
        assignmentRepository.save(assignment);
    }

    @Transactional
    public void deleteAssignment(UUID assignmentId, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId)
                .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);

        for (AssignmentSubmission submission : assignment.getSubmissions()) {
            List<AttachmentResponse> subAttachments = attachmentService.getAttachmentsForEntity(submission.getId(), EntityType.SUBMISSION, user);
            for (AttachmentResponse attachment : subAttachments) {
                attachmentService.deleteAttachmentAsSystem(attachment.getId());
            }
        }

        List<AttachmentResponse> assignmentAttachments = attachmentService.getAttachmentsForEntity(assignmentId, EntityType.ASSIGNMENT, user);
        for (AttachmentResponse attachment : assignmentAttachments) {
            attachmentService.deleteAttachmentAsSystem(attachment.getId());
        }

        assignmentRepository.delete(assignment);
    }
}
