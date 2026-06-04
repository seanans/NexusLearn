package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.LessonCreateRequest;
import com.nexuslearn.api.dtos.LessonResponse;
import com.nexuslearn.api.dtos.LessonUpdateRequest;
import com.nexuslearn.api.dtos.PublishStatusRequest;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<Map<String, UUID>> createLesson(@PathVariable UUID moduleId, @Valid @RequestBody LessonCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        UUID lessonId = lessonService.createLesson(moduleId, request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", lessonId));
    }

    @GetMapping("/modules/{moduleId}/lessons")
    public ResponseEntity<List<LessonResponse>> getLessons(@PathVariable UUID moduleId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(lessonService.getLessonsByModule(moduleId, userDetails.user()));
    }

    @PutMapping("/lessons/{lessonId}")
    public ResponseEntity<Void> updateLesson(@PathVariable UUID lessonId, @Valid @RequestBody LessonUpdateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        lessonService.updateLesson(lessonId, request, userDetails.user());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/lessons/{lessonId}/publish")
    public ResponseEntity<Void> updatePublishStatus(@PathVariable UUID lessonId, @Valid @RequestBody PublishStatusRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        lessonService.updateLessonPublishStatus(lessonId, request.getIsPublished(), userDetails.user());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/lessons/{lessonId}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID lessonId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        lessonService.deleteLesson(lessonId, userDetails.user());
        return ResponseEntity.noContent().build();
    }
}
