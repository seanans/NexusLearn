package com.nexuslearn.api.services.integration;

import com.nexuslearn.api.TestcontainersConfiguration;
import com.nexuslearn.api.dtos.ModuleSummaryProjection;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.*;
import com.nexuslearn.api.services.ModuleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
public class ModuleServiceIT {

    @Autowired private ModuleService moduleService;
    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private CourseMemberRepository courseMemberRepository;
    @Autowired private ModuleRepository moduleRepository;

    private User teacher;
    private User student;
    private Course course;

    @BeforeEach
    void setUp() {
        // 1. Setup Users
        teacher = userRepository.save(User.builder()
                .email("teacher@nexuslearn.com")
                .passwordHash("hashed")
                .firstName("Ada").lastName("Lovelace")
                .build());

        student = userRepository.save(User.builder()
                .email("student@nexuslearn.com")
                .passwordHash("hashed")
                .firstName("Alan").lastName("Turing")
                .build());

        // 2. Setup Course
        course = courseRepository.save(Course.builder()
                .title("Integration Testing 101")
                .description("Advanced Spring Boot")
                .lastActivityAt(LocalDateTime.now())
                .build());

        // 3. Assign Roles
        courseMemberRepository.save(CourseMember.builder()
                .id(new CourseMemberId(teacher.getId(), course.getId()))
                .user(teacher).course(course).role(CourseRole.TEACHER)
                .build());

        courseMemberRepository.save(CourseMember.builder()
                .id(new CourseMemberId(student.getId(), course.getId()))
                .user(student).course(course).role(CourseRole.STUDENT)
                .build());

        // 4. Setup Modules (One Published, One Unpublished)
        moduleRepository.save(Module.builder()
                .course(course).title("Module 1: Basics")
                .orderIndex(1).isPublished(true)
                .build());

        moduleRepository.save(Module.builder()
                .course(course).title("Module 2: Advanced (Draft)")
                .orderIndex(2).isPublished(false)
                .build());
    }

    @Test
    void getModulesByCourse_AsTeacher_ReturnsAllModules() {
        List<ModuleSummaryProjection> modules = moduleService.getModulesByCourse(course.getId(), teacher);

        assertThat(modules).hasSize(2);
        assertThat(modules).extracting(ModuleSummaryProjection::getTitle)
                .containsExactly("Module 1: Basics", "Module 2: Advanced (Draft)");
    }

    @Test
    void getModulesByCourse_AsStudent_ReturnsOnlyPublishedModules() {
        List<ModuleSummaryProjection> modules = moduleService.getModulesByCourse(course.getId(), student);

        assertThat(modules).hasSize(1);
        assertThat(modules.getFirst().getTitle()).isEqualTo("Module 1: Basics");
        assertThat(modules.getFirst().getIsPublished()).isTrue();
    }
}