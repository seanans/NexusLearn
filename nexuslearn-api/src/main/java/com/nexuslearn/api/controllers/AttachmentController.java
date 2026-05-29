package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.AttachmentCreateRequest;
import com.nexuslearn.api.dtos.AttachmentResponse;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.AttachmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public ResponseEntity<AttachmentResponse> linkAttachment(@Valid @RequestBody AttachmentCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        AttachmentResponse response = attachmentService.linkAttachment(request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable UUID attachmentId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        attachmentService.deleteAttachment(attachmentId, userDetails.user());
        return ResponseEntity.noContent().build();
    }
}