package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.SubmissionCreateRequest;
import com.nexuslearn.api.dtos.SubmissionGradeRequest;
import com.nexuslearn.api.dtos.SubmissionResponse;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<SubmissionResponse> submitAssignment(
            @PathVariable UUID assignmentId,
            @Valid @RequestBody SubmissionCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SubmissionResponse response = submissionService.submitAssignment(assignmentId, request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<SubmissionResponse> gradeSubmission(
            @PathVariable UUID submissionId,
            @Valid @RequestBody SubmissionGradeRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        SubmissionResponse response = submissionService.gradeSubmission(submissionId, request, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/assignments/{assignmentId}/submissions")
    public ResponseEntity<List<SubmissionResponse>> getSubmissions(
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return ResponseEntity.ok(submissionService.getSubmissionsForAssignment(assignmentId, userDetails.user()));
    }
}