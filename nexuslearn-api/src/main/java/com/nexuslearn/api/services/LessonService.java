package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.*;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.AttachmentRepository;
import com.nexuslearn.api.repositories.LessonRepository;
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
public class LessonService {
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final CourseSecurityValidator securityValidator;
    private final AttachmentService attachmentService;
    private final AttachmentRepository attachmentRepository;

    @Transactional
    public UUID createLesson(UUID moduleId, LessonCreateRequest request, User user) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));
        securityValidator.validateAccess(module.getCourse().getId(), user, true);

        Integer nextOrderIndex = lessonRepository.findMaxOrderIndexByModuleId(moduleId) + 1;

        Lesson lesson = Lesson.builder().module(module).title(request.getTitle()).content(request.getContent()).orderIndex(nextOrderIndex).isPublished(request.getIsPublished() != null ? request.getIsPublished() : false).availableFrom(request.getAvailableFrom()).build();

        lessonRepository.save(lesson);
        return lesson.getId();
    }

    @Transactional
    public void updateLesson(UUID lessonId, LessonUpdateRequest request, User user) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new AppException("Lesson not found", HttpStatus.NOT_FOUND));
        securityValidator.validateAccess(lesson.getModule().getCourse().getId(), user, true);

        lesson.setTitle(request.getTitle());
        lesson.setContent(request.getContent());
        lesson.setIsPublished(request.getIsPublished() != null ? request.getIsPublished() : false);
        lesson.setAvailableFrom(request.getAvailableFrom());

        lessonRepository.save(lesson);

        if (request.getNewAttachments() != null && !request.getNewAttachments().isEmpty()) {
            for (PendingAttachmentDto stagedFile : request.getNewAttachments()) {
                Attachment newAttachment = new Attachment();
                newAttachment.setEntityId(lesson.getId());
                newAttachment.setEntityType(EntityType.LESSON);
                newAttachment.setFileUrl(stagedFile.getFileUrl());
                newAttachment.setFileName(stagedFile.getFileName());
                newAttachment.setFileType(stagedFile.getFileType());

                attachmentRepository.save(newAttachment);
            }
        }
    }

    @Transactional
    public void deleteLesson(UUID lessonId, User user) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new AppException("Lesson not found", HttpStatus.NOT_FOUND));
        securityValidator.validateAccess(lesson.getModule().getCourse().getId(), user, true);
        List<AttachmentResponse> attachments = attachmentService.getAttachmentsForEntity(lessonId, EntityType.LESSON, user);
        for (AttachmentResponse attachment : attachments) {
            attachmentService.deleteAttachmentAsSystem(attachment.getId());
        }
        lessonRepository.delete(lesson);
    }

    @Transactional
    public void updateLessonPublishStatus(UUID lessonId, Boolean isPublished, User user) {
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new AppException("Lesson not found", HttpStatus.NOT_FOUND));
        securityValidator.validateAccess(lesson.getModule().getCourse().getId(), user, true);

        lesson.setIsPublished(isPublished);
        lessonRepository.save(lesson);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsByModule(UUID moduleId, User user) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));
        CourseRole userRole = securityValidator.getUserRoleInCourse(module.getCourse().getId(), user);

        if (userRole == CourseRole.STUDENT && !module.getIsPublished()) {
            throw new AppException("Access Denied: This module is unpublished", HttpStatus.FORBIDDEN);
        }

        List<Lesson> lessons = (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) ? lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId) : lessonRepository.findVisibleLessonsForStudent(moduleId);

        return lessons.stream().map(l -> LessonResponse.builder().id(l.getId()).title(l.getTitle()).content(l.getContent()).orderIndex(l.getOrderIndex()).published(l.getIsPublished()).availableFrom(l.getAvailableFrom()).createdAt(l.getCreatedAt()).updatedAt(l.getUpdatedAt()).build()).collect(Collectors.toList());
    }
}