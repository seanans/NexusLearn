package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.ChatInboxItemDto;
import com.nexuslearn.api.dtos.ChatMessageRequest;
import com.nexuslearn.api.dtos.ChatMessageResponse;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/inbox")
    public ResponseEntity<List<ChatInboxItemDto>> getInbox(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(chatService.getChatInbox(userDetails.user()));
    }

    @GetMapping("/channels/{channelId}/history")
    public ResponseEntity<Slice<ChatMessageResponse>> getChatHistory(
            @PathVariable UUID channelId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Slice<ChatMessageResponse> historySlice = chatService.getChannelChatHistory(channelId, userDetails.user(), PageRequest.of(page, size));
        return ResponseEntity.ok(historySlice);
    }

    @MessageMapping("/chat/channels/{channelId}")
    public void sendMessage(@DestinationVariable UUID channelId, @Payload ChatMessageRequest request, Principal principal) {
        try {
            ChatMessageResponse response = chatService.processAndSaveMessage(channelId, request, principal);
            messagingTemplate.convertAndSend("/topic/channels/" + channelId, response);
        } catch (Exception e) {
            System.err.println("Failed to process STOMP message for channel " + channelId);
        }
    }

    @PostMapping("/channels/{channelId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID channelId, Principal principal) {
        chatService.markChannelAsRead(channelId, principal);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/channels/reference/{referenceId}")
    public ResponseEntity<Map<String, UUID>> getChannelByReference(
            @PathVariable UUID referenceId,
            @RequestParam(required = false) UUID courseId,
            @RequestParam(required = false) String type,
            Principal principal) {

        UUID channelId = chatService.getOrCreateChannelByReference(referenceId, courseId, type, principal);
        return ResponseEntity.ok(Map.of("channelId", channelId));
    }

    @PostMapping("/channels/direct/{targetUserId}")
    public ResponseEntity<Map<String, UUID>> getOrCreateDirectMessage(
            @PathVariable UUID targetUserId, Principal principal) {
        UUID channelId = chatService.getOrCreateDirectMessageChannel(targetUserId, principal);
        return ResponseEntity.ok(Map.of("channelId", channelId));
    }
}