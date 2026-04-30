package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.CourseCreateRequest;
import com.nexuslearn.api.dtos.CourseResponse;
import com.nexuslearn.api.dtos.CourseUpdateRequest;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.repositories.CourseMemberRepository;
import com.nexuslearn.api.repositories.CourseRepository;
import com.nexuslearn.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;
    private final CourseSecurityValidator securityValidator;

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Course course = Course.builder().title(request.getTitle()).description(request.getDescription()).lastActivityMessage("Course created").lastActivityAt(java.time.LocalDateTime.now()).build();

        course = courseRepository.save(course);

        CourseMemberId memberId = new CourseMemberId(user.getId(), course.getId());
        CourseMember courseMember = CourseMember.builder().id(memberId).user(user).course(course).role(CourseRole.TEACHER).build();
        courseMemberRepository.save(courseMember);

        return CourseResponse.builder().id(course.getId()).title(course.getTitle()).description(course.getDescription()).creatorName(user.getFirstName() + " " + user.getLastName()).lastActivityMessage(course.getLastActivityMessage()).lastActivityAt(course.getLastActivityAt()).build();
    }

    @Transactional
    public CourseResponse updateCourse(UUID courseId, CourseUpdateRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (securityValidator.getUserRoleInCourse(courseId, user) != CourseRole.TEACHER) {
            throw new AppException("Access Denied: Only teachers can update course details", HttpStatus.FORBIDDEN);
        }

        Course course = courseRepository.findById(courseId).orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));

        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setLastActivityMessage("Course details updated");
        course.setLastActivityAt(java.time.LocalDateTime.now());

        course = courseRepository.save(course);
        return CourseResponse.builder().id(course.getId()).title(course.getTitle()).description(course.getDescription()).lastActivityMessage(course.getLastActivityMessage()).lastActivityAt(course.getLastActivityAt()).build();
    }

    @Transactional
    public void addMemberToCourse(UUID courseId, String targetEmail, CourseRole role, String requesterEmail) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));
        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new AppException("Requester not found", HttpStatus.NOT_FOUND));

        CourseRole requesterRole = securityValidator.getUserRoleInCourse(courseId, requester);

        if (requesterRole == CourseRole.STUDENT) {
            throw new AppException("Access Denied: Students cannot add members to the course", HttpStatus.FORBIDDEN);
        }

        if (requesterRole == CourseRole.ASSISTANT && (role == CourseRole.TEACHER || role == CourseRole.ASSISTANT)) {
            throw new AppException("Access Denied: Assistants can only grant Student privileges", HttpStatus.FORBIDDEN);
        }

        User targetUser = userRepository.findByEmail(targetEmail).orElseThrow(() -> new AppException("Target user not found", HttpStatus.NOT_FOUND));
        CourseMemberId targetMemberId = new CourseMemberId(targetUser.getId(), course.getId());

        if (courseMemberRepository.existsById(targetMemberId)) {
            throw new AppException("User is already a member of this course", HttpStatus.CONFLICT);
        }

        CourseMember newMember = CourseMember.builder().id(targetMemberId).user(targetUser).course(course).role(role).build();
        courseMemberRepository.save(newMember);
    }

    @Transactional
    public void removeMemberFromCourse(UUID courseId, String targetEmail, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new AppException("Requester not found", HttpStatus.NOT_FOUND));
        User targetUser = userRepository.findByEmail(targetEmail).orElseThrow(() -> new AppException("Target user not found", HttpStatus.NOT_FOUND));

        CourseRole requesterRole = securityValidator.getUserRoleInCourse(courseId, requester);

        // Cross-removal vs Self-removal logic
        if (!requesterEmail.equals(targetEmail)) {
            if (requesterRole == CourseRole.STUDENT) {
                throw new AppException("Access Denied: Students can only remove themselves", HttpStatus.FORBIDDEN);
            }
            if (requesterRole == CourseRole.ASSISTANT) {
                CourseRole targetRole = securityValidator.getUserRoleInCourse(courseId, targetUser);
                if (targetRole == CourseRole.TEACHER || targetRole == CourseRole.ASSISTANT) {
                    throw new AppException("Access Denied: Assistants can only remove Students", HttpStatus.FORBIDDEN);
                }
            }
        } else {
            if (requesterRole == CourseRole.TEACHER) {
                throw new AppException("Access Denied: Teachers cannot unenroll themselves. You must delete the course.", HttpStatus.FORBIDDEN);
            }
        }

        CourseMemberId targetMemberId = new CourseMemberId(targetUser.getId(), courseId);
        if (!courseMemberRepository.existsById(targetMemberId)) {
            throw new AppException("User is not a member of this course", HttpStatus.NOT_FOUND);
        }

        courseMemberRepository.deleteById(targetMemberId);
    }

    @Transactional(readOnly = true)
    public Slice<CourseResponse> getMyCourses(String email, Pageable pageable) {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        return courseRepository.findDashboardCourses(user.getId(), pageable).map(proj -> CourseResponse.builder().id(proj.getId()).title(proj.getTitle()).description(proj.getDescription()).lastActivityMessage(proj.getLastActivityMessage()).lastActivityAt(proj.getLastActivityAt()).creatorName(proj.getTeacherFirstName() != null ? proj.getTeacherFirstName() + " " + proj.getTeacherLastName() : "No Teacher Assigned").build());
    }

    @Transactional
    public void deleteCourse(UUID courseId, String requesterEmail) {
        User user = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        if (securityValidator.getUserRoleInCourse(courseId, user) != CourseRole.TEACHER) {
            throw new AppException("Access Denied: Only teachers can delete a course", HttpStatus.FORBIDDEN);
        }

        courseRepository.deleteById(courseId);
    }
}
