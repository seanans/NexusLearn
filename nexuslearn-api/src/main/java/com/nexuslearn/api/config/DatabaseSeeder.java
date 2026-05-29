package com.nexuslearn.api.config;

import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.*;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final AttachmentRepository attachmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name:nexuslearn-files}")
    private String bucketName;

    @Value("${minio.url:http://localhost:9000}")
    private String minioBaseUrl;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping data population.");
            return;
        }

        log.info("Starting NexusLearn application data seeding...");

        // 1. Seed Users
        String commonPasswordHash = passwordEncoder.encode("Password123!");

        User teacher = User.builder().email("teacher@nexuslearn.com").passwordHash(commonPasswordHash).firstName("Professor").lastName("Snape").build();
        userRepository.save(teacher);

        User student1 = User.builder().email("student1@nexuslearn.com").passwordHash(commonPasswordHash).firstName("Harry").lastName("Potter").build();
        userRepository.save(student1);

        User student2 = User.builder().email("student2@nexuslearn.com").passwordHash(commonPasswordHash).firstName("Ron").lastName("Weasley").build();
        userRepository.save(student2);

        // 2. Seed Course
        Course course = Course.builder().title("Advanced Software Engineering: JPA & Patterns").description("Master enterprise development using Spring Boot 4, Hibernate performance tuning, and scalable Angular standalone applications.").lastActivityMessage("Course structure initialized by system seeder").lastActivityAt(LocalDateTime.now()).build();
        course = courseRepository.save(course);

        // 3. Assign Members to Course
        CourseMember memberTeacher = CourseMember.builder().id(new CourseMemberId(teacher.getId(), course.getId())).user(teacher).course(course).role(CourseRole.TEACHER).build();
        courseMemberRepository.save(memberTeacher);

        CourseMember memberStudent1 = CourseMember.builder().id(new CourseMemberId(student1.getId(), course.getId())).user(student1).course(course).role(CourseRole.STUDENT).build();
        courseMemberRepository.save(memberStudent1);

        CourseMember memberStudent2 = CourseMember.builder().id(new CourseMemberId(student2.getId(), course.getId())).user(student2).course(course).role(CourseRole.STUDENT).build();
        courseMemberRepository.save(memberStudent2);

        // 4. Seed Module 1 (Published Container)
        Module module1 = Module.builder().course(course).title("Module 1: Relational Persistence Optimization").description("Diving deep into fetch configurations, caching layers, and database migration strategies.").orderIndex(1).isPublished(true).build();
        module1 = moduleRepository.save(module1);

        // Lessons for Module 1
        String lesson1Content = "<p>In this lesson, we study the core differences between LAZY and EAGER extraction types, mapping strategies to avoid runtime exceptions.</p>" +
                "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/tgbNymZ7vqY\" frameborder=\"0\" allowfullscreen></iframe>";

        Lesson lesson1 = Lesson.builder().module(module1).title("Understanding Fetch Joins & Entity Graphs").content(lesson1Content).orderIndex(1).isPublished(true).availableFrom(LocalDateTime.now().minusDays(5)).build();
        lesson1 = lessonRepository.save(lesson1);

        // Attachments for lesson 1 (Only physical cloud files)
        String pdfUrl = createDummyFileInMinio("Lecture_Slides.pdf", "application/pdf", "Dummy PDF Content".getBytes(StandardCharsets.UTF_8));
        attachmentRepository.save(Attachment.builder().entityId(lesson1.getId()).entityType(EntityType.LESSON).fileUrl(pdfUrl).fileName("Lecture_Slides.pdf").fileType("PDF").build());

        Lesson lesson2 = Lesson.builder().module(module1).title("Time-Gated Premium Strategy Content").content("<p>This text content is restricted by scheduled availability bounds.</p>").orderIndex(2).isPublished(true).availableFrom(LocalDateTime.now().plusDays(3)).build();
        lessonRepository.save(lesson2);

        // Assignment for Module 1
        Assignment assignment1 = Assignment.builder().module(module1).title("Lab Assignment: Resolving N+1 Query Anomalies").description("Write a repository tier service layer utilizing JPA Interface Projections to bundle hierarchical domain streams into continuous JSON structures cleanly.").maxScore(100).dueDate(LocalDateTime.now().plusDays(7)).orderIndex(3).isPublished(true).availableFrom(LocalDateTime.now().minusDays(5)).build();
        assignment1 = assignmentRepository.save(assignment1);

        // Attachment for assignment 1
        byte[] dummyImage = Base64.getDecoder().decode("R0lGODlhAQABAAAAACH5BAEKAAEALAAAAAABAAEAAAICTAEAOw==");
        String imageUrl = createDummyFileInMinio("Schema_Architecture.gif", "image/gif", dummyImage);
        attachmentRepository.save(Attachment.builder().entityId(assignment1.getId()).entityType(EntityType.ASSIGNMENT).fileUrl(imageUrl).fileName("Schema_Architecture.gif").fileType("IMAGE").build());

        // 5. Seed Module 2 (Draft/Unpublished Container)
        Module module2 = Module.builder().course(course).title("Module 2: Reactive Pipelines & Messaging [Draft]").description("Exploring asynchronous processing architectures with WebSockets and persistent queues.").orderIndex(2).isPublished(false).build();
        module2 = moduleRepository.save(module2);

        Lesson lesson3 = Lesson.builder().module(module2).title("Introduction to Event-Driven State Replication").content("<p>Unpublished text definition.</p>").orderIndex(1).isPublished(true).build();
        lessonRepository.save(lesson3);

        // 6. Seed Submissions for Assignment 1
        AssignmentSubmission submission1 = AssignmentSubmission.builder().assignment(assignment1).user(student1).submissionText("My implementation handles the batch stitching via custom stream mappings, grouping the entities in memory efficiently.").score(95).feedback("Excellent optimization strategy. Code successfully addresses transaction boundaries.").build();
        submission1.setGradedBy(teacher);
        submission1 = submissionRepository.save(submission1);

        // Attachment for submission 1
        String zipUrl = createDummyFileInMinio("SourceCode.zip", "application/zip", "Dummy Zip Content".getBytes(StandardCharsets.UTF_8));
        attachmentRepository.save(Attachment.builder().entityId(submission1.getId()).entityType(EntityType.SUBMISSION).fileUrl(zipUrl).fileName("Harry_Potter_Lab1_SourceCode.zip").fileType("ZIP").build());

        AssignmentSubmission submission2 = AssignmentSubmission.builder().assignment(assignment1).user(student2).submissionText("Here is my partial solution code stub layout...").score(null).feedback(null).build();
        submissionRepository.save(submission2);

        log.info("NexusLearn data seeding completed successfully.");
    }

    private String createDummyFileInMinio(String fileName, String mimeType, byte[] content) throws Exception {
        String objectName = UUID.randomUUID() + "-" + fileName;

        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(new ByteArrayInputStream(content), content.length, -1).contentType(mimeType).build());

        return minioBaseUrl + "/" + bucketName + "/" + objectName;
    }
}