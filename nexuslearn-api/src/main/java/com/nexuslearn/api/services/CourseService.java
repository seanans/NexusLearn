package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.CourseCreateRequest;
import com.nexuslearn.api.dtos.CourseResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Course;
import com.nexuslearn.api.models.CourseMember;
import com.nexuslearn.api.models.CourseMemberId;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.CourseMemberRepository;
import com.nexuslearn.api.repositories.CourseRepository;
import com.nexuslearn.api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request, String userEmail) {

        User user = userRepository.findByEmail(userEmail).orElseThrow(() -> new AppException("User not found", HttpStatus.NOT_FOUND));

        Course course = Course.builder().title(request.getTitle()).description(request.getDescription()).build();
        course = courseRepository.save(course);

        CourseMemberId memberId = new CourseMemberId(user.getId(), course.getId());
        CourseMember courseMember = CourseMember.builder().id(memberId).user(user).course(course).role("TEACHER").build();
        courseMemberRepository.save(courseMember);

        return mapToResponse(course, user);
    }

    @Transactional
    public void addMemberToCourse(UUID courseId, String targetEmail, String role, String requesterEmail) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));

        User requester = userRepository.findByEmail(requesterEmail).orElseThrow(() -> new AppException("Requester not found", HttpStatus.NOT_FOUND));

        CourseMemberId requesterMemberId = new CourseMemberId(requester.getId(), course.getId());
        CourseMember requesterMember = courseMemberRepository.findById(requesterMemberId).orElseThrow(() -> new AppException("Access Denied: You are not a member of this course", HttpStatus.FORBIDDEN));

        if (!"TEACHER".equalsIgnoreCase(requesterMember.getRole())) {
            throw new AppException("Access Denied: Only teachers can add new members to this course", HttpStatus.FORBIDDEN);
        }

        User targetUser = userRepository.findByEmail(targetEmail).orElseThrow(() -> new AppException("Target user not found", HttpStatus.NOT_FOUND));

        CourseMemberId targetMemberId = new CourseMemberId(targetUser.getId(), course.getId());

        if (courseMemberRepository.existsById(targetMemberId)) {
            throw new AppException("User is already a member of this course", HttpStatus.CONFLICT);
        }

        CourseMember newMember = CourseMember.builder().id(targetMemberId).user(targetUser).course(course).role(role.toUpperCase()).build();

        courseMemberRepository.save(newMember);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> getAllCourses() {
        return courseRepository.findAll().stream().map(course -> mapToResponse(course, null)).collect(Collectors.toList());
    }

    private CourseResponse mapToResponse(Course course, User creator) {
        return CourseResponse.builder().id(course.getId()).title(course.getTitle()).description(course.getDescription()).creatorName(creator != null ? creator.getFirstName() + " " + creator.getLastName() : "Unknown").build();
    }
}
