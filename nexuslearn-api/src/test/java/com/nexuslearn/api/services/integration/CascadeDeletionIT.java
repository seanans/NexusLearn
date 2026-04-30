package com.nexuslearn.api.services.integration;

import com.nexuslearn.api.TestcontainersConfiguration;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.*;
import com.nexuslearn.api.services.CourseService;
import com.nexuslearn.api.services.ModuleService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
public class CascadeDeletionIT {

    @Autowired
    private CourseService courseService;
    @Autowired
    private ModuleService moduleService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseMemberRepository courseMemberRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AssignmentSubmissionRepository submissionRepository;
    @Autowired
    private EntityManager entityManager;

    private User teacher;
    private User student;
    private UUID courseId;
    private UUID moduleId;
    private UUID lessonId;
    private UUID assignmentId;
    private UUID submissionId;

    @BeforeEach
    void setUp() {
        // 1. Setup Users
        teacher = userRepository.save(User.builder().email("teacher.cascade@nexuslearn.com").passwordHash("hash").firstName("A").lastName("B").build());
        student = userRepository.save(User.builder().email("student.cascade@nexuslearn.com").passwordHash("hash").firstName("C").lastName("D").build());

        // 2. Setup Course
        Course course = courseRepository.save(Course.builder().title("Cascade Test Course").lastActivityAt(LocalDateTime.now()).build());
        courseId = course.getId();

        // 3. Setup Members
        courseMemberRepository.save(CourseMember.builder().id(new CourseMemberId(teacher.getId(), courseId)).user(teacher).course(course).role(CourseRole.TEACHER).build());
        courseMemberRepository.save(CourseMember.builder().id(new CourseMemberId(student.getId(), courseId)).user(student).course(course).role(CourseRole.STUDENT).build());

        // 4. Setup Module
        Module module = moduleRepository.save(Module.builder().course(course).title("Module 1").orderIndex(1).isPublished(true).build());
        moduleId = module.getId();

        // 5. Setup Lesson
        Lesson lesson = lessonRepository.save(Lesson.builder().module(module).title("Lesson 1").content("Content").isPublished(true).orderIndex(1).build());
        lessonId = lesson.getId();

        // 6. Setup Assignment
        Assignment assignment = assignmentRepository.save(Assignment.builder().module(module).title("Assignment 1").maxScore(100).dueDate(LocalDateTime.now().plusDays(1)).isPublished(true).orderIndex(2).build());
        assignmentId = assignment.getId();

        // 7. Setup Submission
        AssignmentSubmission submission = submissionRepository.save(AssignmentSubmission.builder().assignment(assignment).user(student).submissionText("My answer").build());
        submissionId = submission.getId();

        // Assert deep hierarchy exists
        assertThat(submissionRepository.count()).isEqualTo(1);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deleteCourse_AsTeacher_CascadesToAllChildren() {
        // Action: Delete the root of the hierarchy
        courseService.deleteCourse(courseId, teacher.getEmail());

        // Flush is required in tests to force Hibernate to send DELETE statements to PostgreSQL immediately
        courseRepository.flush();

        // Verification: The database must be completely devoid of this course's deep data
        assertThat(courseRepository.findById(courseId)).isEmpty();
        assertThat(courseMemberRepository.count()).isEqualTo(0);
        assertThat(moduleRepository.findById(moduleId)).isEmpty();
        assertThat(lessonRepository.findById(lessonId)).isEmpty();
        assertThat(assignmentRepository.findById(assignmentId)).isEmpty();
        assertThat(submissionRepository.findById(submissionId)).isEmpty();

        // Verification: Users must NOT be deleted (ManyToOne boundary)
        assertThat(userRepository.count()).isEqualTo(2);
    }

    @Test
    void deleteModule_AsTeacher_CascadesOnlyToModuleChildren() {
        // Action: Delete the module (middle of the hierarchy)
        moduleService.deleteModule(moduleId, teacher);

        moduleRepository.flush();

        // Verification: Module and its children must be gone
        assertThat(moduleRepository.findById(moduleId)).isEmpty();
        assertThat(lessonRepository.findById(lessonId)).isEmpty();
        assertThat(assignmentRepository.findById(assignmentId)).isEmpty();
        assertThat(submissionRepository.findById(submissionId)).isEmpty();

        // Verification: Course and Members MUST remain intact
        assertThat(courseRepository.findById(courseId)).isPresent();
        assertThat(courseMemberRepository.count()).isEqualTo(2);
    }
}