package com.nexuslearn.api.controllers;

import com.nexuslearn.api.dtos.*;
import com.nexuslearn.api.security.CustomUserDetails;
import com.nexuslearn.api.services.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseResponse> getCourseById(@PathVariable UUID courseId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CourseResponse response = courseService.getCourseById(courseId, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<Slice<CourseResponse>> getMyCourses(@AuthenticationPrincipal CustomUserDetails userDetails, Pageable pageable) {
        Slice<CourseResponse> courses = courseService.getMyCourses(userDetails.user(), pageable);
        return ResponseEntity.ok(courses);
    }

    @GetMapping("/{courseId}/syllabus")
    public ResponseEntity<CourseSyllabusResponse> getCourseSyllabus(@PathVariable UUID courseId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CourseSyllabusResponse response = courseService.getCourseSyllabus(courseId, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{courseId}/lessons/{lessonId}")
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable UUID courseId, @PathVariable UUID lessonId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        LessonResponse response = courseService.getLessonById(courseId, lessonId, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{courseId}/assignments/{assignmentId}")
    public ResponseEntity<AssignmentResponse> getAssignmentById(@PathVariable UUID courseId, @PathVariable UUID assignmentId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        AssignmentResponse response = courseService.getAssignmentById(courseId, assignmentId, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CourseCreateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CourseResponse response = courseService.createCourse(request, userDetails.user());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{courseId}")
    public ResponseEntity<CourseResponse> updateCourse(@PathVariable UUID courseId, @Valid @RequestBody CourseUpdateRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        CourseResponse response = courseService.updateCourse(courseId, request, userDetails.user());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID courseId, @AuthenticationPrincipal CustomUserDetails userDetails) {
        courseService.deleteCourse(courseId, userDetails.user());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{courseId}/members")
    public ResponseEntity<MessageResponse> addCourseMember(@PathVariable UUID courseId, @Valid @RequestBody AddMemberToCourseRequest request, @AuthenticationPrincipal CustomUserDetails userDetails) {
        courseService.addMemberToCourse(courseId, request.getEmail(), request.getRole(), userDetails.user());
        return ResponseEntity.ok(new MessageResponse("Member successfully added to course"));
    }

    @DeleteMapping("/{courseId}/members/{email}")
    public ResponseEntity<MessageResponse> removeCourseMember(@PathVariable UUID courseId, @PathVariable String email, @AuthenticationPrincipal CustomUserDetails userDetails) {
        courseService.removeMemberFromCourse(courseId, email, userDetails.user());
        return ResponseEntity.ok(new MessageResponse("Member successfully removed from course"));
    }

}
