package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.AttachmentCreateRequest;
import com.nexuslearn.api.dtos.ChatMessageRequest;
import com.nexuslearn.api.dtos.ChatMessageResponse;
import com.nexuslearn.api.dtos.PendingAttachmentDto;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.ChatMessage;
import com.nexuslearn.api.models.Course;
import com.nexuslearn.api.models.EntityType;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.ChatMessageRepository;
import com.nexuslearn.api.repositories.CourseRepository;
import com.nexuslearn.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final CourseRepository courseRepository;
    private final CourseSecurityValidator securityValidator;
    private final AttachmentService attachmentService;

    public Slice<ChatMessageResponse> getCourseChatHistory(UUID courseId, User user, Pageable pageable) {
        securityValidator.validateAccess(courseId, user, false);
        Slice<ChatMessage> messages = chatMessageRepository.findRecentMessagesByCourse(courseId, pageable);
        return messages.map(msg -> mapToResponse(msg, user));
    }

    @Transactional
    public ChatMessageResponse processAndSaveMessage(UUID courseId, ChatMessageRequest request, Principal principal) {
        validateMessagePayload(request);
        User sender = extractSender((UsernamePasswordAuthenticationToken) principal);

        securityValidator.validateAccess(courseId, sender, false);
        Course course = courseRepository.findById(courseId).orElseThrow();

        String messageContent = (request.getContent() == null) ? "" : request.getContent();

        ChatMessage message = ChatMessage.builder()
                .course(course)
                .sender(sender)
                .content(messageContent)
                .build();

        message = chatMessageRepository.save(message);

        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (PendingAttachmentDto file : request.getAttachments()) {
                AttachmentCreateRequest attachReq = new AttachmentCreateRequest();
                attachReq.setEntityId(message.getId());
                attachReq.setEntityType(EntityType.MESSAGE);
                attachReq.setFileUrl(file.getFileUrl());
                attachReq.setFileName(file.getFileName());
                attachReq.setFileType(file.getFileType());

                attachmentService.linkAttachment(attachReq, sender);
            }
        }

        return mapToResponse(message, sender);
    }

    private void validateMessagePayload(ChatMessageRequest request) {
        boolean hasText = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasFiles = request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasText && !hasFiles) {
            throw new AppException("Message must contain text or at least one file", HttpStatus.BAD_REQUEST);
        }
    }

    private User extractSender(UsernamePasswordAuthenticationToken principal) {
        if (principal == null || principal.getPrincipal() == null) {
            throw new AppException("Authentication context is missing", HttpStatus.UNAUTHORIZED);
        }
        CustomUserDetails userDetails = (CustomUserDetails) principal.getPrincipal();
        User sender = userDetails.user();

        if (sender == null) {
            throw new AppException("User profile could not be loaded", HttpStatus.UNAUTHORIZED);
        }

        return sender;
    }

    private ChatMessageResponse mapToResponse(ChatMessage message, User user) {
        boolean isEdited = message.getUpdatedAt() != null &&
                message.getUpdatedAt().isAfter(message.getCreatedAt());

        return ChatMessageResponse.builder()
                .id(message.getId())
                .courseId(message.getCourse().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isEdited(isEdited)
                .attachments(attachmentService.getAttachmentsForEntity(message.getId(), EntityType.MESSAGE, user))
                .build();
    }
}
