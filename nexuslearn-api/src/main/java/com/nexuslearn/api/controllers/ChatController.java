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

    @GetMapping("/courses/{courseId}/history")
    public ResponseEntity<Slice<ChatMessageResponse>> getChatHistory(
            @PathVariable UUID courseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Slice<ChatMessageResponse> historySlice = chatService.getCourseChatHistory(courseId, userDetails.user(), PageRequest.of(page, size));
        return ResponseEntity.ok(historySlice);
    }

    @MessageMapping("/chat/{courseId}")
    public void sendMessage(@DestinationVariable UUID courseId, @Payload ChatMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.processAndSaveMessage(courseId, request, principal);
        messagingTemplate.convertAndSend("/topic/course/" + courseId, response);
    }
}