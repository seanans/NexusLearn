package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.*;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.repositories.*;
import com.nexuslearn.api.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChatService {
    private final ChatMessageRepository chatMessageRepository;
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
    private final CourseSecurityValidator securityValidator;
    private final AttachmentService attachmentService;
    private final CourseRepository courseRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Slice<ChatMessageResponse> getChannelChatHistory(UUID channelId, User user, Pageable pageable) {
        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new AppException("Channel not found", HttpStatus.NOT_FOUND));

        validateChannelAccess(channel, user, false);

        Slice<ChatMessage> messages = chatMessageRepository.findRecentMessagesByChannel(channelId, pageable);

        if (messages.isEmpty()) {
            return new SliceImpl<>(Collections.emptyList(), pageable, false);
        }

        List<UUID> messageIds = messages.getContent().stream().map(ChatMessage::getId).toList();
        Map<UUID, List<AttachmentResponse>> attachmentsMap = attachmentService.getAttachmentsForEntities(messageIds, EntityType.MESSAGE);

        return messages.map(msg -> {
            List<AttachmentResponse> msgAttachments = attachmentsMap.getOrDefault(msg.getId(), Collections.emptyList());
            return mapToResponse(msg, msgAttachments);
        });
    }

    @Transactional(readOnly = true)
    public List<ChatInboxItemDto> getChatInbox(User user) {
        List<Object[]> rawInboxData = channelRepository.findChatInboxDataByUserId(user.getId());

        return rawInboxData.stream().map(row -> {
            UUID channelId = (UUID) row[0];
            String channelName = (String) row[1];
            String rawContent = (String) row[2];
            LocalDateTime timestamp = (LocalDateTime) row[3];
            ChannelType type = ChannelType.valueOf((String) row[4]);
            UUID courseId = (UUID) row[5];
            String courseTitle = (String) row[6];
            UUID referenceId = (UUID) row[7];
            String fileName = (String) row[8];
            boolean hasUnread = (Boolean) row[9];

            if (channelName != null) {
                channelName = channelName.replaceFirst("^(?i)(Discussion|Q&A|Feedback):\\s*", "");
            }

            String snippet = rawContent;
            if (snippet == null || snippet.trim().isEmpty()) {
                snippet = (fileName != null) ? "📎 " + fileName : "New message";
            }

            if (snippet.length() > 40) snippet = snippet.substring(0, 37) + "...";

            return ChatInboxItemDto.builder().channelId(channelId).channelName(channelName).latestMessageSnippet(snippet).latestMessageTimestamp(timestamp).channelType(type).courseId(courseId).courseTitle(courseTitle).referenceId(referenceId).hasUnread(hasUnread).build();
        }).toList();
    }

    @Transactional
    public ChatMessageResponse processAndSaveMessage(UUID channelId, ChatMessageRequest request, Principal principal) {
        validateMessagePayload(request);
        User sender = extractSender((UsernamePasswordAuthenticationToken) principal);

        Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new AppException("Channel not found", HttpStatus.NOT_FOUND));

        validateChannelAccess(channel, sender, true);

        String messageContent = (request.getContent() == null) ? "" : request.getContent();

        ChatMessage message = ChatMessage.builder().channel(channel).sender(sender).content(messageContent).build();

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

        List<AttachmentResponse> attachments = attachmentService.getAttachmentsForEntity(message.getId(), EntityType.MESSAGE, sender);
        return mapToResponse(message, attachments);
    }

    @Transactional
    public void markChannelAsRead(UUID channelId, Principal principal) {
        User user = extractSender((UsernamePasswordAuthenticationToken) principal);
        ChannelMemberId memberId = new ChannelMemberId(channelId, user.getId());

        channelMemberRepository.findById(memberId).ifPresentOrElse(member -> {
            member.setLastReadAt(LocalDateTime.now());
            channelMemberRepository.save(member);
        }, () -> {
            Channel channel = channelRepository.findById(channelId).orElseThrow(() -> new AppException("Channel not found", HttpStatus.NOT_FOUND));

            ChannelMember newTracker = ChannelMember.builder().id(memberId).channel(channel).user(user).joinedAt(LocalDateTime.now()).lastReadAt(LocalDateTime.now()).build();

            channelMemberRepository.save(newTracker);
        });
    }

    @Transactional
    public UUID getOrCreateDirectMessageChannel(UUID targetUserId, Principal principal) {
        User currentUser = extractSender((UsernamePasswordAuthenticationToken) principal);
        if (currentUser.getId().equals(targetUserId)) {
            throw new AppException("Cannot create a DM with yourself", HttpStatus.BAD_REQUEST);
        }

        Optional<UUID> existingChannelId = channelRepository.findDirectChannelBetweenUsers(currentUser.getId(), targetUserId);
        if (existingChannelId.isPresent()) {
            return existingChannelId.get();
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new AppException("Target user not found", HttpStatus.NOT_FOUND));

        try {
            Channel newChannel = Channel.builder()
                    .type(ChannelType.DIRECT)
                    .build();

            newChannel = channelRepository.saveAndFlush(newChannel);

            ChannelMember member1 = ChannelMember.builder()
                    .id(new ChannelMemberId(newChannel.getId(), currentUser.getId()))
                    .channel(newChannel).user(currentUser)
                    .joinedAt(LocalDateTime.now()).lastReadAt(LocalDateTime.now()).build();

            ChannelMember member2 = ChannelMember.builder()
                    .id(new ChannelMemberId(newChannel.getId(), targetUser.getId()))
                    .channel(newChannel).user(targetUser)
                    .joinedAt(LocalDateTime.now()).lastReadAt(LocalDateTime.now()).build();

            channelMemberRepository.saveAllAndFlush(List.of(member1, member2));

            return newChannel.getId();

        } catch (DataIntegrityViolationException e) {
            return channelRepository.findDirectChannelBetweenUsers(currentUser.getId(), targetUserId)
                    .orElseThrow(() -> new AppException("Failed to retrieve concurrent DM channel", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    @Transactional
    public UUID getOrCreateChannelByReference(UUID referenceId, UUID courseId, String typeStr, Principal principal) {
        Optional<Channel> existing = channelRepository.findFirstByReferenceId(referenceId);
        if (existing.isPresent()) {
            return existing.get().getId();
        }

        if (courseId == null || typeStr == null) {
            throw new AppException("Channel not found and missing parameters for creation", HttpStatus.NOT_FOUND);
        }

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));
        ChannelType type = ChannelType.valueOf(typeStr);

        try {
            Channel newChannel = Channel.builder().course(course).name(type == ChannelType.ASSIGNMENT_PRIVATE ? "Private Feedback" : "Discussion").type(type).referenceId(referenceId).build();

            newChannel = channelRepository.save(newChannel);

            if (type == ChannelType.ASSIGNMENT_PRIVATE) {
                User currentUser = extractSender((UsernamePasswordAuthenticationToken) principal);

                boolean isTeacher = false;
                try {
                    securityValidator.validateAccess(courseId, currentUser, true);
                    isTeacher = true;
                } catch (AppException ignored) {
                }
                if (!isTeacher) {
                    ChannelMember member = ChannelMember.builder().id(new ChannelMemberId(newChannel.getId(), currentUser.getId())).channel(newChannel).user(currentUser).joinedAt(LocalDateTime.now()).lastReadAt(LocalDateTime.now()).build();
                    channelMemberRepository.save(member);
                } else {

                    Channel finalNewChannel = newChannel;
                    assignmentSubmissionRepository.findById(referenceId).ifPresent(sub -> {
                        ChannelMember member = ChannelMember.builder().id(new ChannelMemberId(finalNewChannel.getId(), sub.getUser().getId())).channel(finalNewChannel).user(sub.getUser()).joinedAt(LocalDateTime.now()).lastReadAt(LocalDateTime.now()).build();
                        channelMemberRepository.save(member);
                    });
                }
            }
            return newChannel.getId();
        } catch (DataIntegrityViolationException e) {
            return channelRepository.findFirstByReferenceId(referenceId).map(Channel::getId).orElseThrow(() -> new AppException("Failed to retrieve concurrent channel", HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private void validateChannelAccess(Channel channel, User user, boolean isSending) {
        ChannelType type = channel.getType();

        if (type == ChannelType.COURSE || type == ChannelType.LESSON || type == ChannelType.ASSIGNMENT_PUBLIC) {
            securityValidator.validateAccess(channel.getCourse().getId(), user, false);
        } else if (type == ChannelType.ANNOUNCEMENT) {
            // Read access
            securityValidator.validateAccess(channel.getCourse().getId(), user, false);
            // Write access
            if (isSending) {
                securityValidator.validateAccess(channel.getCourse().getId(), user, true);
            }
        } else if (type == ChannelType.DIRECT || type == ChannelType.GROUP || type == ChannelType.ASSIGNMENT_PRIVATE) {
            if (type == ChannelType.ASSIGNMENT_PRIVATE) {
                try {
                    securityValidator.validateAccess(channel.getCourse().getId(), user, true);
                    return;
                } catch (AppException ignored) {
                }
            }

            boolean isMember = channelMemberRepository.existsByChannelIdAndUserId(channel.getId(), user.getId());
            if (!isMember) {
                throw new AppException("You do not have access to this private channel.", HttpStatus.FORBIDDEN);
            }
        }
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

    private ChatMessageResponse mapToResponse(ChatMessage message, List<AttachmentResponse> attachments) {
        boolean isEdited = message.getUpdatedAt() != null && message.getUpdatedAt().isAfter(message.getCreatedAt());

        return ChatMessageResponse.builder().id(message.getId()).channelId(message.getChannel().getId()).senderId(message.getSender().getId()).senderName(message.getSender().getFirstName() + " " + message.getSender().getLastName()).content(message.getContent()).createdAt(message.getCreatedAt()).updatedAt(message.getUpdatedAt()).isEdited(isEdited).attachments(attachments).build();
    }
}
