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
import java.io.File;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final ChatMessageRepository chatMessageRepository;
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
        if (userRepository.count() > 0) return;

        // 1. Seed 10 Users with REAL names
        String[][] realNames = {
                {"Alan", "Turing"},      // Index 0: Teacher
                {"Grace", "Hopper"},     // Index 1: TA
                {"Alice", "Smith"},      // Index 2: Student
                {"Bob", "Johnson"},      // Index 3: Student
                {"Charlie", "Brown"},    // Index 4: Student
                {"David", "Miller"},     // Index 5: Student
                {"Eve", "Davis"},        // Index 6: Student
                {"Frank", "Wilson"},     // Index 7: Student
                {"George", "Taylor"},    // Index 8: Student
                {"Hannah", "Moore"}      // Index 9: Student
        };

        User[] users = new User[10];
        for (int i = 0; i < 10; i++) {
            users[i] = userRepository.save(User.builder()
                    .email(realNames[i][0].toLowerCase() + "@nexuslearn.com")
                    .passwordHash(passwordEncoder.encode("Password123!"))
                    .firstName(realNames[i][0])
                    .lastName(realNames[i][1])
                    .build());
        }

        // 2. Seed 3 Courses
        for (int c = 0; c < 3; c++) {
            Course course = courseRepository.save(Course.builder()
                    .title("Enterprise Architecture " + (c + 101))
                    .description("Advanced concepts in software engineering, system design, and scalable networking.")
                    .build());

            // Assign users to courses with their correct hierarchical roles
            for (int i = 0; i < users.length; i++) {
                CourseRole role = (i == 0) ? CourseRole.TEACHER : (i == 1) ? CourseRole.ASSISTANT : CourseRole.STUDENT;
                courseMemberRepository.save(CourseMember.builder()
                        .id(new CourseMemberId(users[i].getId(), course.getId()))
                        .user(users[i])
                        .course(course)
                        .role(role)
                        .build());
            }

            // 3. Seed Modules, Lessons, Assignments, and Submissions
            for (int m = 1; m <= 2; m++) { // 2 Modules per course
                Module module = moduleRepository.save(Module.builder()
                        .course(course)
                        .title("Module " + m + ": Core Systems")
                        .description("Understanding the fundamental paradigms of distributed architecture.")
                        .orderIndex(m)
                        .isPublished(true).build());

                // Lesson with a sample attachment
                Lesson lesson = lessonRepository.save(Lesson.builder()
                        .module(module)
                        .title("Lesson " + m + " Overview")
                        .content("<p>Please review the attached presentation slides before our next seminar.</p>")
                        .orderIndex(1)
                        .isPublished(true).build());

                String lessonFileUrl = uploadRealFile("sample.pdf", "application/pdf");
                attachmentRepository.save(Attachment.builder()
                        .entityId(lesson.getId()).entityType(EntityType.LESSON)
                        .fileUrl(lessonFileUrl).fileName("Lecture_Slides_M" + m + ".pdf").fileType("PDF").build());

                // Assignment with a sample attachment
                Assignment assignment = assignmentRepository.save(Assignment.builder()
                        .module(module)
                        .title("Lab Assignment " + m)
                        .description("Complete the system design document based on the requirements attached.")
                        .dueDate(LocalDateTime.now().plusDays(7))
                        .orderIndex(2)
                        .maxScore(100)
                        .isPublished(true).build());

                String assignFileUrl = uploadRealFile("sample.pdf", "application/pdf");
                attachmentRepository.save(Attachment.builder()
                        .entityId(assignment.getId()).entityType(EntityType.ASSIGNMENT)
                        .fileUrl(assignFileUrl).fileName("Lab_Requirements.pdf").fileType("PDF").build());

                // ---- REALISTIC SUBMISSION STATES ----

                // Submission 1: Graded (Alice - Excellent)
                AssignmentSubmission aliceSub = submissionRepository.save(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .user(users[2])
                        .submissionText("Here is my completed lab implementation. I utilized standard DTO projections to bypass the persistent context overhead.")
                        .score(95)
                        .feedback("Excellent architectural choices, Alice. Very efficient.")
                        .gradedBy(users[0]) // Graded by Professor Alan Turing
                        .submittedAt(LocalDateTime.now().minusDays(2))
                        .gradedAt(LocalDateTime.now().minusDays(1))
                        .build());

                // Attach a ZIP to Alice's submission
                String aliceFileUrl = uploadRealFile("sample.zip", "application/zip");
                attachmentRepository.save(Attachment.builder()
                        .entityId(aliceSub.getId()).entityType(EntityType.SUBMISSION)
                        .fileUrl(aliceFileUrl).fileName("Alice_SourceCode.zip").fileType("ZIP").build());

                // Submission 2: Ungraded / Pending Review (Bob - Just submitted)
                submissionRepository.save(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .user(users[3])
                        .submissionText("Professor, I struggled a bit with the MinIO bucket configuration but the fallback text generation is working as expected.")
                        .submittedAt(LocalDateTime.now().minusHours(2))
                        // Note: Score, feedback, gradedBy, and gradedAt are intentionally NULL
                        .build());

                // Submission 3: Graded / Late (Charlie)
                submissionRepository.save(AssignmentSubmission.builder()
                        .assignment(assignment)
                        .user(users[4])
                        .submissionText("Sorry for the delay, had internet issues.")
                        .score(75)
                        .feedback("Good work, but -10 points for late submission.")
                        .gradedBy(users[1]) // Graded by TA Grace Hopper
                        .submittedAt(LocalDateTime.now().plusDays(8)) // Intentionally past the dueDate
                        .gradedAt(LocalDateTime.now().plusDays(9))
                        .build());
            }

            // 4. Seed 100 Messages per course (Original Chat Logic)
            for (int m = 0; m < 100; m++) {
                ChatMessage msg = chatMessageRepository.save(ChatMessage.builder()
                        .course(course)
                        .sender(users[m % 10])
                        .content("Message #" + m + " - Discussing the current module material.")
                        .build());

                // Every 10th message, add an attachment
                if (m % 10 == 0) {
                    String fileName = (m % 30 == 0) ? "sample.pdf" : (m % 30 == 10) ? "sample.png" : "sample.zip";
                    String mimeType = (m % 30 == 0) ? "application/pdf" : (m % 30 == 10) ? "image/png" : "application/zip";

                    String fileUrl = uploadRealFile(fileName, mimeType);
                    attachmentRepository.save(Attachment.builder()
                            .entityId(msg.getId())
                            .entityType(EntityType.MESSAGE)
                            .fileUrl(fileUrl)
                            .fileName(fileName)
                            .fileType(fileName.contains("pdf") ? "PDF" : fileName.contains("png") ? "IMAGE" : "ZIP")
                            .build());
                }
            }
        }
        log.info("Seeding complete: 1 Teacher, 1 Assistant, 8 Students across 3 courses with rich LMS content.");
    }

    private String uploadRealFile(String fileName, String mimeType) throws Exception {
        File file = new File("seeding-files/" + fileName);
        if (!file.exists()) return minioBaseUrl + "/" + bucketName + "/fallback_" + System.currentTimeMillis() + ".txt";

        byte[] content = Files.readAllBytes(file.toPath());
        String objectName = UUID.randomUUID() + "-" + fileName;
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .contentType(mimeType).build());
        return minioBaseUrl + "/" + bucketName + "/" + objectName;
    }
}