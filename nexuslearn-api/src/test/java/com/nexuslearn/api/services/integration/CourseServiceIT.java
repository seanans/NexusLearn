package com.nexuslearn.api.services.integration;

import com.nexuslearn.api.TestcontainersConfiguration;
import com.nexuslearn.api.dtos.CourseCreateRequest;
import com.nexuslearn.api.dtos.CourseResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.CourseMember;
import com.nexuslearn.api.models.CourseMemberId;
import com.nexuslearn.api.models.CourseRole;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.CourseMemberRepository;
import com.nexuslearn.api.repositories.UserRepository;
import com.nexuslearn.api.services.CourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
public class CourseServiceIT {

    @Autowired
    private CourseService courseService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseMemberRepository courseMemberRepository;

    private User primaryTeacher;
    private User assistant;
    private User student;
    private User targetUser;

    private CourseResponse activeCourse;

    @BeforeEach
    void setUp() {
        primaryTeacher = createSavedUser("teacher@nexuslearn.com", "Linus", "Torvalds");
        assistant = createSavedUser("assistant@nexuslearn.com", "Ada", "Lovelace");
        student = createSavedUser("student@nexuslearn.com", "Kevin", "Mitnick");
        targetUser = createSavedUser("target@nexuslearn.com", "Grace", "Hopper");

        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Advanced RBAC");

        // FIX: Passed 'primaryTeacher' object instead of email
        activeCourse = courseService.createCourse(request, primaryTeacher);

        // FIX: Passed user objects as the final requester parameter
        courseService.addMemberToCourse(activeCourse.getId(), assistant.getEmail(), CourseRole.ASSISTANT, primaryTeacher);
        courseService.addMemberToCourse(activeCourse.getId(), student.getEmail(), CourseRole.STUDENT, primaryTeacher);
    }

    private User createSavedUser(String email, String first, String last) {
        return userRepository.save(User.builder().email(email).passwordHash("hashed").firstName(first).lastName(last).build());
    }

    @Test
    void addMember_TeacherAddsAssistant_Success() {
        courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.ASSISTANT, primaryTeacher);
        assertRoleInDatabase(targetUser.getId(), CourseRole.ASSISTANT);
    }

    @Test
    void addMember_AssistantAddsStudent_Success() {
        courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.STUDENT, assistant);
        assertRoleInDatabase(targetUser.getId(), CourseRole.STUDENT);
    }

    @Test
    void addMember_AssistantAttemptsToAddTeacher_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.TEACHER, assistant))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_AssistantAttemptsToAddAssistant_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.ASSISTANT, assistant))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_StudentAttemptsToAddTeacher_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.TEACHER, student))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_StudentAttemptsToAddAssistant_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.ASSISTANT, student))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_StudentAttemptsToAddStudent_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.STUDENT, student))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void addMember_UserAlreadyInCourse_ThrowsConflict() {
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), student.getEmail(), CourseRole.STUDENT, primaryTeacher))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.CONFLICT);
    }

    @Test
    void addMember_RequesterNotInCourse_ThrowsForbidden() {
        User outsider = createSavedUser("outsider@nexuslearn.com", "John", "Doe");
        assertThatThrownBy(() -> courseService.addMemberToCourse(activeCourse.getId(), targetUser.getEmail(), CourseRole.STUDENT, outsider))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void removeMember_TeacherRemovesStudent_Success() {
        courseService.removeMemberFromCourse(activeCourse.getId(), student.getEmail(), primaryTeacher);
        Optional<CourseMember> memberOpt = courseMemberRepository.findById(new CourseMemberId(student.getId(), activeCourse.getId()));
        assertThat(memberOpt).isEmpty();
    }

    @Test
    void removeMember_StudentUnenrollsSelf_Success() {
        courseService.removeMemberFromCourse(activeCourse.getId(), student.getEmail(), student);
        Optional<CourseMember> memberOpt = courseMemberRepository.findById(new CourseMemberId(student.getId(), activeCourse.getId()));
        assertThat(memberOpt).isEmpty();
    }

    @Test
    void removeMember_StudentAttemptsToRemoveTeacher_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.removeMemberFromCourse(activeCourse.getId(), primaryTeacher.getEmail(), student))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void removeMember_AssistantAttemptsToRemoveTeacher_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.removeMemberFromCourse(activeCourse.getId(), primaryTeacher.getEmail(), assistant))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void removeMember_TeacherAttemptsToUnenrollSelf_ThrowsForbidden() {
        assertThatThrownBy(() -> courseService.removeMemberFromCourse(activeCourse.getId(), primaryTeacher.getEmail(), primaryTeacher))
                .isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    private void assertRoleInDatabase(java.util.UUID userId, CourseRole expectedRole) {
        Optional<CourseMember> memberOpt = courseMemberRepository.findById(new CourseMemberId(userId, activeCourse.getId()));
        assertThat(memberOpt).isPresent();
        assertThat(memberOpt.get().getRole()).isEqualTo(expectedRole);
    }
}