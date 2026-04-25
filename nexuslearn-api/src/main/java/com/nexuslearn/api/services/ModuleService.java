package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.ModuleSummaryProjection;
import com.nexuslearn.api.models.*;
import com.nexuslearn.api.dtos.ModuleCreateRequest;
import com.nexuslearn.api.dtos.ModuleResponse;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.repositories.CourseMemberRepository;
import com.nexuslearn.api.repositories.CourseRepository;
import com.nexuslearn.api.repositories.ModuleRepository;
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
public class ModuleService {
    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final CourseMemberRepository courseMemberRepository;
    private final UserRepository userRepository;

    private void validateCourseAccess(UUID courseId, User user, boolean requireElevatedPrivileges) {
        CourseMemberId memberId = new CourseMemberId(user.getId(), courseId);
        CourseMember member = courseMemberRepository.findById(memberId)
                .orElseThrow(() -> new AppException("Access Denied: You are not a member of this course", HttpStatus.FORBIDDEN));

        if (requireElevatedPrivileges) {
            CourseRole role = member.getRole();
            if (role != CourseRole.TEACHER && role != CourseRole.ASSISTANT) {
                throw new AppException("Access Denied: Insufficient privileges", HttpStatus.FORBIDDEN);
            }
        }
    }

    @Transactional
    public ModuleResponse createModule(UUID courseId, ModuleCreateRequest request, User user) {

        validateCourseAccess(courseId, user, true);

        Course courseReference = courseRepository.getReferenceById(courseId);

        Integer nextOrderIndex = moduleRepository.findMaxOrderIndexByCourseId(courseId) + 1;

        Module module = Module.builder()
                .course(courseReference)
                .title(request.getTitle())
                .description(request.getDescription())
                .orderIndex(nextOrderIndex)
                .isPublished(false)
                .build();

        module = moduleRepository.save(module);

        return ModuleResponse.builder()
                .id(module.getId())
                .title(module.getTitle())
                .description(module.getDescription())
                .orderIndex(module.getOrderIndex())
                .isPublished(module.getIsPublished())
                .createdAt(module.getCreatedAt())
                .updatedAt(module.getUpdatedAt())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ModuleSummaryProjection> getModulesByCourse(UUID courseId, User user) {
        validateCourseAccess(courseId, user, false);
        return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }
}
