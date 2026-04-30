package com.nexuslearn.api.services.integration;

import com.nexuslearn.api.TestcontainersConfiguration;
import com.nexuslearn.api.dtos.SubmissionCreateRequest;
import com.nexuslearn.api.dtos.SubmissionGradeRequest;
import com.nexuslearn.api.dtos.SubmissionResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.*;
import com.nexuslearn.api.services.SubmissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
public class SubmissionServiceIT {

    @Autowired
    private SubmissionService submissionService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CourseRepository courseRepository;
    @Autowired
    private CourseMemberRepository courseMemberRepository;
    @Autowired
    private ModuleRepository moduleRepository;
    @Autowired
    private AssignmentRepository assignmentRepository;
    @Autowired
    private AssignmentSubmissionRepository submissionRepository;

    private User student;
    private User teacher;
    private User assistant;
    private Assignment assignment;
    private AssignmentSubmission existingSubmission;

    @BeforeEach
    void setUp() {
        // 1. Setup Users
        student = userRepository.save(User.builder().email("student.concurrent@nexuslearn.com").passwordHash("hashed").firstName("Grace").lastName("Hopper").build());

        teacher = userRepository.save(User.builder().email("teacher.grading@nexuslearn.com").passwordHash("hashed").firstName("John").lastName("von Neumann").build());

        assistant = userRepository.save(User.builder().email("assistant.grading@nexuslearn.com").passwordHash("hashed").firstName("Margaret").lastName("Hamilton").build());

        // 2. Setup Hierarchy
        Course course = courseRepository.save(Course.builder().title("Concurrency & Grading 101").lastActivityAt(LocalDateTime.now()).build());

        // 3. Assign Roles
        courseMemberRepository.save(CourseMember.builder().id(new CourseMemberId(student.getId(), course.getId())).user(student).course(course).role(CourseRole.STUDENT).build());

        courseMemberRepository.save(CourseMember.builder().id(new CourseMemberId(teacher.getId(), course.getId())).user(teacher).course(course).role(CourseRole.TEACHER).build());

        courseMemberRepository.save(CourseMember.builder().id(new CourseMemberId(assistant.getId(), course.getId())).user(assistant).course(course).role(CourseRole.ASSISTANT).build());

        Module module = moduleRepository.save(Module.builder().course(course).title("Module 1").orderIndex(1).isPublished(true).build());

        assignment = assignmentRepository.save(Assignment.builder().module(module).title("Final Project").maxScore(100).dueDate(LocalDateTime.now().plusDays(7)).orderIndex(1).isPublished(true).build());
        // 4. Pre-seed a submission for grading tests
        existingSubmission = submissionRepository.save(AssignmentSubmission.builder().assignment(assignment).user(student).submissionText("Initial submission for grading").build());
    }

    @AfterEach
    void tearDown() {
        submissionRepository.deleteAll();
        assignmentRepository.deleteAll();
        moduleRepository.deleteAll();
        courseMemberRepository.deleteAll();
        courseRepository.deleteAll();
        userRepository.deleteAll();
    }

    // --- CONCURRENCY TESTS ---

    @Test
    void submitAssignment_ConcurrentDoubleSubmission_OnlyOneSucceeds() throws InterruptedException {
        // Delete the pre-seeded submission for this specific test
        submissionRepository.deleteAll();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        SubmissionCreateRequest request = new SubmissionCreateRequest();
        request.setSubmissionText("Here is my concurrent submission.");

        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            tasks.add(() -> {
                try {
                    latch.await();
                    submissionService.submitAssignment(assignment.getId(), request, student);
                    successCount.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    conflictCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return null;
            });
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Callable<Void> task : tasks) {
            futures.add(executor.submit(task));
        }

        Thread.sleep(100);
        latch.countDown();

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (ExecutionException e) {
            }
        }
        executor.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(submissionRepository.findByAssignmentId(assignment.getId())).hasSize(1);
    }

    // --- GRADING ENGINE TESTS ---

    @Test
    void submitAssignment_Resubmission_ClearsPreviousGradeAndFeedback() {
        // Pre-condition: The teacher grades the initial submission
        existingSubmission.setScore(90);
        existingSubmission.setFeedback("Good initial attempt, but needs more detail.");
        submissionRepository.save(existingSubmission);

        // Action: The student resubmits their work
        SubmissionCreateRequest request = new SubmissionCreateRequest();
        request.setSubmissionText("Here is my updated answer based on your feedback.");
        SubmissionResponse response = submissionService.submitAssignment(assignment.getId(), request, student);

        // Verification: The response DTO must reflect wiped grading data
        assertThat(response.getScore()).isNull();
        assertThat(response.getFeedback()).isNull();
        assertThat(response.getSubmissionText()).isEqualTo("Here is my updated answer based on your feedback.");

        // Database Verification: Prove the old grade is physically gone from PostgreSQL
        AssignmentSubmission dbSub = submissionRepository.findById(existingSubmission.getId()).orElseThrow();
        assertThat(dbSub.getScore()).isNull();
        assertThat(dbSub.getFeedback()).isNull();
    }

    @Test
    void gradeSubmission_AsTeacher_ValidScore_Success() {
        SubmissionGradeRequest request = new SubmissionGradeRequest();
        request.setScore(95);
        request.setFeedback("Excellent logic, minor formatting issues.");

        SubmissionResponse response = submissionService.gradeSubmission(existingSubmission.getId(), request, teacher);

        assertThat(response.getScore()).isEqualTo(95);
        assertThat(response.getFeedback()).isEqualTo("Excellent logic, minor formatting issues.");

        // Verify persistence
        AssignmentSubmission dbSubmission = submissionRepository.findById(existingSubmission.getId()).orElseThrow();
        assertThat(dbSubmission.getScore()).isEqualTo(95);
    }

    @Test
    void gradeSubmission_AsStudent_ThrowsForbidden() {
        SubmissionGradeRequest request = new SubmissionGradeRequest();
        request.setScore(100);

        assertThatThrownBy(() -> submissionService.gradeSubmission(existingSubmission.getId(), request, student)).isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.FORBIDDEN);
    }

    @Test
    void gradeSubmission_ScoreExceedsMaxScore_ThrowsBadRequest() {
        SubmissionGradeRequest request = new SubmissionGradeRequest();
        request.setScore(150); // Assignment maxScore is 100

        assertThatThrownBy(() -> submissionService.gradeSubmission(existingSubmission.getId(), request, teacher)).isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST).hasMessageContaining("Score exceeds maximum");
    }

    @Test
    void gradeSubmission_AsAssistant_ValidScore_Success() {
        SubmissionGradeRequest request = new SubmissionGradeRequest();
        request.setScore(85);
        request.setFeedback("Solid attempt. Approved by TA.");

        // Execute as Assistant
        SubmissionResponse response = submissionService.gradeSubmission(existingSubmission.getId(), request, assistant);

        assertThat(response.getScore()).isEqualTo(85);
        assertThat(response.getFeedback()).isEqualTo("Solid attempt. Approved by TA.");

        // Verify persistence
        AssignmentSubmission dbSubmission = submissionRepository.findById(existingSubmission.getId()).orElseThrow();
        assertThat(dbSubmission.getScore()).isEqualTo(85);
    }

    @Test
    void gradeSubmission_AsAssistant_ScoreExceedsMaxScore_ThrowsBadRequest() {
        SubmissionGradeRequest request = new SubmissionGradeRequest();
        request.setScore(105); // Assignment maxScore is 100

        assertThatThrownBy(() -> submissionService.gradeSubmission(existingSubmission.getId(), request, assistant)).isInstanceOf(AppException.class).hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST).hasMessageContaining("Score exceeds maximum");
    }
}