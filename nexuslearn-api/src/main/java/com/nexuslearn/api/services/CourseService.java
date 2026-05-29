package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.*;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CourseService {
    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final AssignmentRepository assignmentRepository;
    private final CourseSecurityValidator securityValidator;
    private final AttachmentService attachmentService;

    @Transactional(readOnly = true)
    public CourseResponse getCourseById(UUID courseId, User user) {
        CourseRole role = securityValidator.getUserRoleInCourse(courseId, user);
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));
        return CourseResponse.builder().id(course.getId()).title(course.getTitle()).description(course.getDescription()).lastActivityMessage(course.getLastActivityMessage()).lastActivityAt(course.getLastActivityAt()).currentUserRole(role).build();
    }

    @Transactional(readOnly = true)
    public CourseSyllabusResponse getCourseSyllabus(UUID courseId, User user) {
        CourseRole role = securityValidator.getUserRoleInCourse(courseId, user);

        List<Module> modules;
        if (role == CourseRole.TEACHER || role == CourseRole.ASSISTANT) {
            modules = moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        } else {
            modules = moduleRepository.findByCourseIdAndIsPublishedTrueOrderByOrderIndexAsc(courseId);
        }

        if (modules.isEmpty()) {
            return new CourseSyllabusResponse(courseId, java.util.Collections.emptyList());
        }

        List<UUID> moduleIds = modules.stream().map(Module::getId).toList();

        List<Lesson> allLessons;
        List<Assignment> allAssignments;

        if (role == CourseRole.TEACHER || role == CourseRole.ASSISTANT) {
            allLessons = lessonRepository.findByModuleIdIn(moduleIds);
            allAssignments = assignmentRepository.findByModuleIdIn(moduleIds);
        } else {
            allLessons = lessonRepository.findVisibleByModuleIdIn(moduleIds);
            allAssignments = assignmentRepository.findVisibleByModuleIdIn(moduleIds);
        }

        Map<UUID, List<Lesson>> lessonsByModule = allLessons.stream().collect(Collectors.groupingBy(l -> l.getModule().getId()));
        Map<UUID, List<Assignment>> assignmentsByModule = allAssignments.stream().collect(Collectors.groupingBy(a -> a.getModule().getId()));

        List<CourseSyllabusResponse.SyllabusModuleDto> moduleDtos = modules.stream().map(module -> {
            UUID mId = module.getId();

            Stream<CourseSyllabusResponse.SyllabusItemDto> lessonItems = lessonsByModule.getOrDefault(mId, java.util.Collections.emptyList()).stream().map(l -> new CourseSyllabusResponse.SyllabusItemDto(l.getId(), l.getTitle(), ItemType.LESSON, l.getOrderIndex()));

            Stream<CourseSyllabusResponse.SyllabusItemDto> assignmentItems = assignmentsByModule.getOrDefault(mId, java.util.Collections.emptyList()).stream().map(a -> new CourseSyllabusResponse.SyllabusItemDto(a.getId(), a.getTitle(), ItemType.ASSIGNMENT, a.getOrderIndex()));

            List<CourseSyllabusResponse.SyllabusItemDto> combinedItems = Stream.concat(lessonItems, assignmentItems).sorted(java.util.Comparator.comparing(CourseSyllabusResponse.SyllabusItemDto::orderIndex)).toList();

            return new CourseSyllabusResponse.SyllabusModuleDto(mId, module.getTitle(), module.getOrderIndex(), combinedItems);
        }).toList();

        return new CourseSyllabusResponse(courseId, moduleDtos);
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(UUID courseId, UUID lessonId, User user) {
        CourseRole role = securityValidator.getUserRoleInCourse(courseId, user);
        Lesson lesson;

        if (role == CourseRole.TEACHER || role == CourseRole.ASSISTANT) {
            lesson = lessonRepository.findByIdAndModule_Course_Id(lessonId, courseId).orElseThrow(() -> new AppException("Lesson not found", HttpStatus.NOT_FOUND));
        } else {
            lesson = lessonRepository.findVisibleByIdAndCourseId(lessonId, courseId).orElseThrow(() -> new AppException("Lesson not found or not yet available", HttpStatus.FORBIDDEN));
        }

        return LessonResponse.builder().id(lesson.getId()).title(lesson.getTitle()).content(lesson.getContent()).orderIndex(lesson.getOrderIndex()).isPublished(lesson.getIsPublished()).createdAt(lesson.getCreatedAt()).updatedAt(lesson.getUpdatedAt()).attachments(attachmentService.getAttachmentsForEntity(lessonId, EntityType.LESSON, user)).build();
    }

    @Transactional(readOnly = true)
    public AssignmentResponse getAssignmentById(UUID courseId, UUID assignmentId, User user) {
        CourseRole role = securityValidator.getUserRoleInCourse(courseId, user);
        Assignment assignment;
        if (role == CourseRole.TEACHER || role == CourseRole.ASSISTANT) {
            assignment = assignmentRepository.findByIdAndModule_Course_Id(assignmentId, courseId).orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));
        } else {
            assignment = assignmentRepository.findVisibleByIdAndCourseId(assignmentId, courseId).orElseThrow(() -> new AppException("Assignment not found or not yet available", HttpStatus.FORBIDDEN));
        }

        return AssignmentResponse.builder().id(assignment.getId()).title(assignment.getTitle()).description(assignment.getDescription()).maxScore(assignment.getMaxScore()).dueDate(assignment.getDueDate()).orderIndex(assignment.getOrderIndex()).isPublished(assignment.getIsPublished()).createdAt(assignment.getCreatedAt()).updatedAt(assignment.getUpdatedAt()).attachments(attachmentService.getAttachmentsForEntity(assignmentId, EntityType.ASSIGNMENT, user)).build();
    }

    @Transactional
    public CourseResponse createCourse(CourseCreateRequest request, User user) {
        Course course = Course.builder().title(request.getTitle()).description(request.getDescription()).lastActivityMessage("Course created").lastActivityAt(java.time.LocalDateTime.now()).build();

        course = courseRepository.save(course);

        CourseMemberId memberId = new CourseMemberId(user.getId(), course.getId());
        CourseMember courseMember = CourseMember.builder().id(memberId).user(user).course(course).role(CourseRole.TEACHER).build();
        courseMemberRepository.save(courseMember);

        return CourseResponse.builder().id(course.getId()).title(course.getTitle()).description(course.getDescription()).creatorName(user.getFirstName() + " " + user.getLastName()).lastActivityMessage(course.getLastActivityMessage()).lastActivityAt(course.getLastActivityAt()).build();
    }

    @Transactional
    public CourseResponse updateCourse(UUID courseId, CourseUpdateRequest request, User user) {
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
    public void addMemberToCourse(UUID courseId, String targetEmail, CourseRole role, User requester) {
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new AppException("Course not found", HttpStatus.NOT_FOUND));
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
    public void removeMemberFromCourse(UUID courseId, String targetEmail, User requester) {
        User targetUser = userRepository.findByEmail(targetEmail).orElseThrow(() -> new AppException("Target user not found", HttpStatus.NOT_FOUND));

        CourseRole requesterRole = securityValidator.getUserRoleInCourse(courseId, requester);

        if (!requester.getEmail().equals(targetEmail)) {
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
    public Slice<CourseResponse> getMyCourses(User user, Pageable pageable) {

        return courseRepository.findDashboardCourses(user.getId(), pageable).map(proj -> CourseResponse.builder().id(proj.getId()).title(proj.getTitle()).description(proj.getDescription()).lastActivityMessage(proj.getLastActivityMessage()).lastActivityAt(proj.getLastActivityAt()).creatorName(proj.getTeacherFirstName() != null ? proj.getTeacherFirstName() + " " + proj.getTeacherLastName() : "No Teacher Assigned").build());
    }

    @Transactional
    public void deleteCourse(UUID courseId, User user) {
        if (securityValidator.getUserRoleInCourse(courseId, user) != CourseRole.TEACHER) {
            throw new AppException("Access Denied: Only teachers can delete a course", HttpStatus.FORBIDDEN);
        }

        courseRepository.deleteById(courseId);
    }
}
