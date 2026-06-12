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

    // V2 Chat Repositories
    private final ChannelRepository channelRepository;
    private final ChannelMemberRepository channelMemberRepository;
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

        // 1. Seed 10 Users
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

        User teacherAlan = users[0];

        // 2. Platform-Wide V2 Channels (Not directly tied to a specific course)

        // --- DIRECT CHATS (DMs) ---
        Channel dmAliceAlan = createChannel(ChannelType.DIRECT, null, null, null);
        addChannelMember(dmAliceAlan, teacherAlan);
        addChannelMember(dmAliceAlan, users[2]); // Alice

        sendMessage(dmAliceAlan, users[2], "Professor Turing, I was wondering if we could schedule a quick 1-on-1 to discuss my thesis topic?");
        sendMessage(dmAliceAlan, teacherAlan, "Of course, Alice. I have office hours on Thursday at 3 PM. Does that work for you?");

        Channel dmBobAlan = createChannel(ChannelType.DIRECT, null, null, null);
        addChannelMember(dmBobAlan, teacherAlan);
        addChannelMember(dmBobAlan, users[3]); // Bob

        ChatMessage bobMsg = sendMessage(dmBobAlan, users[3], "Professor, I'm getting a NullPointerException in the MinIO config. I attached a screenshot of the logs.");
        attachFileToEntity(bobMsg.getId(), EntityType.MESSAGE, "sample.png", "image/png");

        sendMessage(dmBobAlan, teacherAlan, "Bob, please check if your bucket name environment variable is properly injected. Send me the stack trace if it persists.");

        // --- STUDY GROUP ---
        Channel studyGroup = createChannel(ChannelType.GROUP, "Advanced Algorithms Study Squad", null, null);
        addChannelMember(studyGroup, users[4]); // Charlie
        addChannelMember(studyGroup, users[5]); // David
        addChannelMember(studyGroup, teacherAlan); // Alan is observing/invited

        sendMessage(studyGroup, users[4], "Hey guys, did Prof Turing say we are allowed to use standard DTOs or do we have to use Records?");
        sendMessage(studyGroup, users[5], "I think he said Records are preferred.");
        sendMessage(studyGroup, teacherAlan, "I am in this group chat, gentlemen. Records are indeed preferred for immutable data carriers. Good luck studying.");


        // 3. Seed 3 Courses
        for (int c = 0; c < 3; c++) {
            Course course = courseRepository.save(Course.builder()
                    .title("Enterprise Architecture " + (c + 101))
                    .description("Advanced concepts in software engineering, system design, and scalable networking.")
                    .build());

            for (int i = 0; i < users.length; i++) {
                CourseRole role = (i == 0) ? CourseRole.TEACHER : (i == 1) ? CourseRole.ASSISTANT : CourseRole.STUDENT;
                courseMemberRepository.save(CourseMember.builder()
                        .id(new CourseMemberId(users[i].getId(), course.getId()))
                        .user(users[i])
                        .course(course)
                        .role(role)
                        .build());
            }

            // --- COURSE CHANNELS ---
            Channel courseLobby = createChannel(ChannelType.COURSE, course.getTitle() + " Main Lobby", course, null);
            Channel announcements = createChannel(ChannelType.ANNOUNCEMENT, "Official Announcements", course, null);

            // Populate Announcement Channel (Teacher only)
            sendMessage(announcements, teacherAlan, "Welcome to " + course.getTitle() + ". Please review the syllabus in Module 1. All assignments must be submitted via the platform.");
            sendMessage(announcements, teacherAlan, "Reminder: Midterm will be held next Tuesday. Study materials are attached to Lesson 2.");

            // Populate Course Lobby
            sendMessage(courseLobby, users[6], "Professor Turing, will the midterm be open book?");
            sendMessage(courseLobby, teacherAlan, "No, Eve. The midterm is closed book, but you may bring one cheat sheet.");
            sendMessage(courseLobby, users[7], "Professor, where can I find the link to the Zoom lectures?");
            sendMessage(courseLobby, teacherAlan, "Frank, the Zoom links are pinned in the Announcements channel.");


            // 4. Seed Modules, Lessons, Assignments
            for (int m = 1; m <= 2; m++) {
                Module module = moduleRepository.save(Module.builder()
                        .course(course)
                        .title("Module " + m + ": Core Systems")
                        .description("Understanding the fundamental paradigms of distributed architecture.")
                        .orderIndex(m)
                        .isPublished(true).build());

                // --- LESSON ---
                Lesson lesson = lessonRepository.save(Lesson.builder()
                        .module(module)
                        .title("Lesson " + m + " Overview")
                        .content("<p>Please review the attached presentation slides.</p>")
                        .orderIndex(1)
                        .isPublished(true).build());

                attachFileToEntity(lesson.getId(), EntityType.LESSON, "sample.pdf", "application/pdf");

                // Create Lesson Channel
                Channel lessonChat = createChannel(ChannelType.LESSON, "Discussion: " + lesson.getTitle(), course, lesson.getId());
                sendMessage(lessonChat, users[8], "Professor Turing, on slide 14, are we assuming a distributed lock, or a local mutex?");
                sendMessage(lessonChat, teacherAlan, "Excellent question, George. We are assuming a Redis-backed distributed lock to prevent race conditions across server nodes.");

                // --- ASSIGNMENT ---
                Assignment assignment = assignmentRepository.save(Assignment.builder()
                        .module(module)
                        .title("Lab Assignment " + m)
                        .description("Complete the system design document based on the requirements attached.")
                        .dueDate(LocalDateTime.now().plusDays(7))
                        .orderIndex(2)
                        .maxScore(100)
                        .isPublished(true).build());

                attachFileToEntity(assignment.getId(), EntityType.ASSIGNMENT, "sample.pdf", "application/pdf");

                // Create Assignment Public Q&A Channel
                Channel assignmentPublicChat = createChannel(ChannelType.ASSIGNMENT_PUBLIC, "Q&A: " + assignment.getTitle(), course, assignment.getId());
                sendMessage(assignmentPublicChat, users[9], "Professor Alan, is the deadline for this strictly at midnight, or before class tomorrow?");
                sendMessage(assignmentPublicChat, teacherAlan, "Hannah, the platform will lock submissions exactly at 23:59 tonight. Please do not wait until the last minute.");

                // --- ASSIGNMENT PRIVATE FEEDBACK CHANNELS & SUBMISSIONS ---
                for (int i = 2; i < users.length; i++) { // For every student
                    User student = users[i];

                    if (i == 2) { // Alice
                        AssignmentSubmission aliceSub = submissionRepository.save(AssignmentSubmission.builder()
                                .assignment(assignment).user(student)
                                .submissionText("Here is my completed lab implementation.")
                                .score(95).feedback("Excellent architectural choices, Alice. Very efficient.")
                                .gradedBy(teacherAlan).submittedAt(LocalDateTime.now().minusDays(2))
                                .gradedAt(LocalDateTime.now().minusDays(1)).build());

                        // FIXED: Use Submission ID for Private Feedback!
                        Channel privateFeedback = createChannel(ChannelType.ASSIGNMENT_PRIVATE, "Feedback: " + student.getFirstName(), course, aliceSub.getId());
                        addChannelMember(privateFeedback, teacherAlan);
                        addChannelMember(privateFeedback, student);

                        attachFileToEntity(aliceSub.getId(), EntityType.SUBMISSION, "sample.zip", "application/zip");

                        ChatMessage aliceMsg = sendMessage(privateFeedback, student, "Professor, I left some comments in the code. I also attached a diagram.");
                        attachFileToEntity(aliceMsg.getId(), EntityType.MESSAGE, "sample.png", "image/png");

                        sendMessage(privateFeedback, teacherAlan, "I saw them, Alice. Very thorough work. I've posted your grade.");

                    } else if (i == 4) { // Charlie
                        AssignmentSubmission charlieSub = submissionRepository.save(AssignmentSubmission.builder()
                                .assignment(assignment).user(student)
                                .submissionText("Sorry for the delay, had internet issues.")
                                .score(75).feedback("Good work, but -10 points for late submission.")
                                .gradedBy(users[1]).submittedAt(LocalDateTime.now().plusDays(8))
                                .gradedAt(LocalDateTime.now().plusDays(9)).build());

                        // FIXED: Use Submission ID for Private Feedback!
                        Channel privateFeedback = createChannel(ChannelType.ASSIGNMENT_PRIVATE, "Feedback: " + student.getFirstName(), course, charlieSub.getId());
                        addChannelMember(privateFeedback, teacherAlan);
                        addChannelMember(privateFeedback, student);

                        sendMessage(privateFeedback, student, "Professor Turing, I know this is late, my router broke. Will you still accept it?");
                        sendMessage(privateFeedback, teacherAlan, "Charlie, I will accept it with a late penalty. I am assigning Grace (TA) to grade this.");
                    }
                }
            }
        }
        log.info("V2 Seeding complete: Polymorphic Channels, Channel Members, and realistic chat histories have been established.");
    }

    // --- Helper Methods ---

    private Channel createChannel(ChannelType type, String name, Course course, UUID referenceId) {
        return channelRepository.save(Channel.builder()
                .type(type)
                .name(name)
                .course(course)
                .referenceId(referenceId)
                .build());
    }

    private void addChannelMember(Channel channel, User user) {
        channelMemberRepository.save(ChannelMember.builder()
                .id(new ChannelMemberId(channel.getId(), user.getId()))
                .channel(channel)
                .user(user)
                .build());
    }

    private ChatMessage sendMessage(Channel channel, User sender, String content) {
        return chatMessageRepository.save(ChatMessage.builder()
                .channel(channel)
                .sender(sender)
                .content(content)
                .build());
    }

    private void attachFileToEntity(UUID entityId, EntityType type, String fileName, String mimeType) throws Exception {
        String fileUrl = uploadRealFile(fileName, mimeType);
        String fileType = fileName.contains(".pdf") ? "PDF" : fileName.contains(".zip") ? "ZIP" : "IMAGE";

        attachmentRepository.save(Attachment.builder()
                .entityId(entityId)
                .entityType(type)
                .fileUrl(fileUrl)
                .fileName(fileName)
                .fileType(fileType)
                .build());
    }

    private String uploadRealFile(String fileName, String mimeType) throws Exception {
        File file = new File("seeding-files/" + fileName);
        if (!file.exists()) {
            log.warn("Seeding file missing: {}. Using fallback text file.", file.getAbsolutePath());
            return minioBaseUrl + "/" + bucketName + "/fallback_" + System.currentTimeMillis() + ".txt";
        }

        byte[] content = Files.readAllBytes(file.toPath());
        String objectName = UUID.randomUUID() + "-" + fileName;
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName).object(objectName)
                .stream(new ByteArrayInputStream(content), content.length, -1)
                .contentType(mimeType).build());
        return minioBaseUrl + "/" + bucketName + "/" + objectName;
    }
}