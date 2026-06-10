package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.AttachmentCreateRequest;
import com.nexuslearn.api.dtos.AttachmentResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Attachment;
import com.nexuslearn.api.models.EntityType;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final CourseSecurityValidator securityValidator;

    @Transactional
    public AttachmentResponse linkAttachment(AttachmentCreateRequest request, User user) {
        securityValidator.validateAttachmentUpload(request.getEntityId(), request.getEntityType(), user);

        Attachment attachment = Attachment.builder().entityId(request.getEntityId()).entityType(request.getEntityType()).fileUrl(request.getFileUrl()).fileName(request.getFileName()).fileType(request.getFileType()).build();

        attachment = attachmentRepository.save(attachment);
        return mapToResponse(attachment);
    }

    public List<AttachmentResponse> getAttachmentsForEntity(UUID entityId, EntityType entityType, User currentUser) {
        securityValidator.validateAttachmentView(entityId, entityType, currentUser);

        return attachmentRepository.findByEntityIdAndEntityType(entityId, entityType).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public Map<UUID, List<AttachmentResponse>> getAttachmentsForEntities(List<UUID> entityIds, EntityType entityType) {
        List<Attachment> attachments = attachmentRepository.findByEntityIdInAndEntityType(entityIds, entityType);

        return attachments.stream()
                .collect(Collectors.groupingBy(
                        Attachment::getEntityId,
                        Collectors.mapping(this::mapToResponse, Collectors.toList())
                ));
    }

    @Transactional
    public void deleteAttachment(UUID attachmentId, User currentUser) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new AppException("Attachment not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAttachmentUpload(attachment.getEntityId(), attachment.getEntityType(), currentUser);

        String objectName = extractObjectName(attachment.getFileUrl());
        fileStorageService.deleteFile(objectName);
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteAttachmentAsSystem(UUID attachmentId) {
        Attachment attachment = attachmentRepository.findById(attachmentId).orElseThrow(() -> new AppException("Attachment not found", HttpStatus.NOT_FOUND));
        String objectName = extractObjectName(attachment.getFileUrl());
        fileStorageService.deleteFile(objectName);
        attachmentRepository.delete(attachment);
    }

    private AttachmentResponse mapToResponse(Attachment attachment) {
        String fullUrl = attachment.getFileUrl();
        String[] urlParts = fullUrl.split("nexuslearn-files/");
        String objectName = urlParts.length > 1 ? urlParts[1] : fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
        String secureReadUrl = fileStorageService.generatePreSignedDownloadUrl(objectName);

        return AttachmentResponse.builder().id(attachment.getId()).entityId(attachment.getEntityId()).entityType(attachment.getEntityType()).fileUrl(secureReadUrl).fileName(attachment.getFileName()).fileType(attachment.getFileType()).createdAt(attachment.getCreatedAt()).build();
    }

    private String extractObjectName(String fullUrl) {
        if (fullUrl == null) return "";
        String[] urlParts = fullUrl.split("nexuslearn-files/");
        return urlParts.length > 1 ? urlParts[1] : fullUrl.substring(fullUrl.lastIndexOf("/") + 1);
    }
}