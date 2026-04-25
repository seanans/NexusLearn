package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.ModuleCreateRequest;
import com.nexuslearn.api.dtos.ModuleResponse;
import com.nexuslearn.api.dtos.ModuleSummaryProjection;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.ModuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses/{courseId}/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping
    public ResponseEntity<ModuleResponse> createModule(
            @PathVariable UUID courseId,
            @Valid @RequestBody ModuleCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        ModuleResponse response = moduleService.createModule(courseId, request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ModuleSummaryProjection>> getModules(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<ModuleSummaryProjection> responses = moduleService.getModulesByCourse(courseId, userDetails.user());
        return ResponseEntity.ok(responses);
    }
}