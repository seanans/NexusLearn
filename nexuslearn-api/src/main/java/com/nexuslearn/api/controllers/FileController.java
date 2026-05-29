package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.PresignedUrlResponse;
import com.nexuslearn.api.models.EntityType;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;

    @GetMapping("/upload-url")
    public ResponseEntity<PresignedUrlResponse> getUploadTicket(@RequestParam String fileName, @RequestParam UUID entityId, @RequestParam EntityType entityType, @AuthenticationPrincipal CustomUserDetails userDetails) {
        PresignedUrlResponse response = fileStorageService.generatePreSignedUploadUrl(fileName, entityId, entityType, userDetails.user());
        return ResponseEntity.ok(response);
    }
}