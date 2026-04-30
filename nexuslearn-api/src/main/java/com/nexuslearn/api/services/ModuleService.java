package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.ModuleCreateRequest;
import com.nexuslearn.api.dtos.ModuleResponse;
import com.nexuslearn.api.dtos.ModuleSummaryProjection;
import com.nexuslearn.api.dtos.ModuleUpdateRequest;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Course;
import com.nexuslearn.api.models.CourseRole;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.CourseRepository;
import com.nexuslearn.api.repositories.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModuleService {
    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final CourseSecurityValidator securityValidator;

    @Transactional
    public ModuleResponse createModule(UUID courseId, ModuleCreateRequest request, User user) {

        securityValidator.validateAccess(courseId, user, true);

        Course courseReference = courseRepository.getReferenceById(courseId);

        Integer nextOrderIndex = moduleRepository.findMaxOrderIndexByCourseId(courseId) + 1;

        Module module = Module.builder().course(courseReference).title(request.getTitle()).description(request.getDescription()).orderIndex(nextOrderIndex).isPublished(false).build();

        module = moduleRepository.save(module);

        return ModuleResponse.builder().id(module.getId()).title(module.getTitle()).description(module.getDescription()).orderIndex(module.getOrderIndex()).isPublished(module.getIsPublished()).createdAt(module.getCreatedAt()).updatedAt(module.getUpdatedAt()).build();
    }

    @Transactional(readOnly = true)
    public List<ModuleSummaryProjection> getModulesByCourse(UUID courseId, User user) {
        CourseRole role = securityValidator.getUserRoleInCourse(courseId, user);

        if (role == CourseRole.TEACHER || role == CourseRole.ASSISTANT) {
            return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
        } else {
            return moduleRepository.findByCourseIdAndIsPublishedTrueOrderByOrderIndexAsc(courseId);
        }
    }

    @Transactional
    public ModuleResponse updateModule(UUID moduleId, ModuleUpdateRequest request, User user) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(module.getCourse().getId(), user, true);

        module.setTitle(request.getTitle());
        module.setDescription(request.getDescription());
        module.setIsPublished(request.getIsPublished());

        module = moduleRepository.save(module);
        return ModuleResponse.builder()
                .id(module.getId()).title(module.getTitle()).description(module.getDescription())
                .orderIndex(module.getOrderIndex()).isPublished(module.getIsPublished())
                .createdAt(module.getCreatedAt()).updatedAt(module.getUpdatedAt()).build();
    }

    @Transactional
    public void deleteModule(UUID moduleId, User user) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(module.getCourse().getId(), user, true);
        moduleRepository.delete(module);
    }
}
