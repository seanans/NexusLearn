package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.*;
import com.nexuslearn.api.services.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseCreateRequest request, Authentication authentication) {
        CourseResponse response = courseService.createCourse(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable UUID courseId, @Valid @RequestBody CourseUpdateRequest request, Authentication authentication) {
        CourseResponse response = courseService.updateCourse(courseId, request, authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID courseId, Authentication authentication) {
        courseService.deleteCourse(courseId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/members")
    public ResponseEntity<MessageResponse> addCourseMember(@PathVariable UUID courseId, @Valid @RequestBody AddMemberToCourseRequest request, Authentication authentication) {
        courseService.addMemberToCourse(courseId, request.getEmail(), request.getRole(), authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Member successfully added to course"));
    }

    @DeleteMapping("/{courseId}/members/{email}")
    public ResponseEntity<MessageResponse> removeCourseMember(@PathVariable UUID courseId, @PathVariable String email, Authentication authentication) {
        courseService.removeMemberFromCourse(courseId, email, authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Member successfully removed from course"));
    }

    @GetMapping("/me")
    public ResponseEntity<Slice<CourseResponse>> getMyCourses(Authentication authentication, Pageable pageable) {
        Slice<CourseResponse> courses = courseService.getMyCourses(authentication.getName(), pageable);
        return ResponseEntity.ok(courses);
    }
}
