package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.LessonCreateRequest;
import com.nexuslearn.api.dtos.LessonSummaryProjection;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.LessonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules/{moduleId}/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;

    @PostMapping
    public ResponseEntity<Void> createLesson(@PathVariable UUID moduleId, @Valid @RequestBody LessonCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {

        lessonService.createLesson(moduleId, request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<LessonSummaryProjection>> getLessons(@PathVariable UUID moduleId, @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(lessonService.getLessonsByModule(moduleId, userDetails.user()));
    }
}
