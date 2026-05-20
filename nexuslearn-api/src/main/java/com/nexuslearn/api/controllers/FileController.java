package com.nexuslearn.api.controllers;

import com.nexuslearn.api.services.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileStorageService fileStorageService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        String fileUrl = fileStorageService.uploadFile(file);
        return ResponseEntity.ok(Map.of("fileUrl", fileUrl));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFile(@RequestParam String fileUrl) {
        fileStorageService.deleteFile(fileUrl);
        return ResponseEntity.noContent().build();
    }
}