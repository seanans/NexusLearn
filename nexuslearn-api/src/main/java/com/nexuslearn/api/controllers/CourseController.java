package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.AddMemberToCourseRequest;
import com.nexuslearn.api.dtos.CourseCreateRequest;
import com.nexuslearn.api.dtos.CourseResponse;
import com.nexuslearn.api.dtos.MessageResponse;
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

    @PostMapping("/{courseId}/members")
    public ResponseEntity<MessageResponse> addCourseMember(@PathVariable UUID courseId, @Valid @RequestBody AddMemberToCourseRequest request, Authentication authentication) {
        courseService.addMemberToCourse(courseId, request.getEmail(), request.getRole(), authentication.getName());
        return ResponseEntity.ok(new MessageResponse("Member successfully added to course"));
    }

    @GetMapping("/me")
    public ResponseEntity<Slice<CourseResponse>> getMyCourses(Authentication authentication, Pageable pageable) {
        Slice<CourseResponse> courses = courseService.getMyCourses(authentication.getName(), pageable);
        return ResponseEntity.ok(courses);
    }
}
