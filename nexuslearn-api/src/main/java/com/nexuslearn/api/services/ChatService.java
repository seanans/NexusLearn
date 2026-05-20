package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.ChatMessageRequest;
import com.nexuslearn.api.dtos.ChatMessageResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.ChatMessage;
import com.nexuslearn.api.models.Course;
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

    public Slice<ChatMessageResponse> getCourseChatHistory(UUID courseId, User user, Pageable pageable) {
        securityValidator.validateAccess(courseId, user, false);
        Slice<ChatMessage> messages = chatMessageRepository.findRecentMessagesByCourse(courseId, pageable);
        return messages.map(this::mapToResponse);
    }

    @Transactional
    public ChatMessageResponse processAndSaveMessage(UUID courseId, ChatMessageRequest request, Principal principal) {

        validateMessagePayload(request);
        User sender = extractSender((UsernamePasswordAuthenticationToken) principal);

        securityValidator.validateAccess(courseId, sender, false);
        Course course = courseRepository.findById(courseId).orElseThrow();

        ChatMessage message = ChatMessage.builder()
                .course(course)
                .sender(sender)
                .content(request.getContent())
                .fileUrl(request.getFileUrl())
                .build();

        return mapToResponse(chatMessageRepository.save(message));
    }

    private void validateMessagePayload(ChatMessageRequest request) {
        if ((request.getContent() == null || request.getContent().isBlank()) && request.getFileUrl() == null) {
            throw new AppException("Message cannot be completely empty", HttpStatus.BAD_REQUEST);
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

    private ChatMessageResponse mapToResponse(ChatMessage message) {
        boolean isEdited = message.getUpdatedAt() != null &&
                message.getUpdatedAt().isAfter(message.getCreatedAt());

        return ChatMessageResponse.builder()
                .id(message.getId())
                .courseId(message.getCourse().getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName())
                .content(message.getContent())
                .fileUrl(message.getFileUrl())
                .createdAt(message.getCreatedAt())
                .updatedAt(message.getUpdatedAt())
                .isEdited(isEdited)
                .build();
    }
}
