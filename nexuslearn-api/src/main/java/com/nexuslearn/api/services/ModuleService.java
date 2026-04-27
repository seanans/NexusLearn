package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.ModuleCreateRequest;
import com.nexuslearn.api.dtos.ModuleResponse;
import com.nexuslearn.api.dtos.ModuleSummaryProjection;
import com.nexuslearn.api.models.Course;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.CourseRepository;
import com.nexuslearn.api.repositories.ModuleRepository;
import lombok.RequiredArgsConstructor;
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
        securityValidator.validateAccess(courseId, user, false);
        return moduleRepository.findByCourseIdOrderByOrderIndexAsc(courseId);
    }
}
