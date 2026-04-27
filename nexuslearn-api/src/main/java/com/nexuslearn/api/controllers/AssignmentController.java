package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.AssignmentCreateRequest;
import com.nexuslearn.api.dtos.AssignmentSummaryProjection;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/modules/{moduleId}/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    public ResponseEntity<Void> createAssignment(
            @PathVariable UUID moduleId,
            @Valid @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        assignmentService.createAssignment(moduleId, request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping
    public ResponseEntity<List<AssignmentSummaryProjection>> getAssignments(
            @PathVariable UUID moduleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(assignmentService.getAssignmentsByModule(moduleId, userDetails.user()));
    }
}
