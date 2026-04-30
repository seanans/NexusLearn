package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.AssignmentCreateRequest;
import com.nexuslearn.api.dtos.AssignmentSummaryProjection;
import com.nexuslearn.api.dtos.AssignmentUpdateRequest;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Assignment;
import com.nexuslearn.api.models.CourseRole;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.AssignmentRepository;
import com.nexuslearn.api.repositories.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final ModuleRepository moduleRepository;
    private final CourseSecurityValidator securityValidator;

    @Transactional
    public void createAssignment(UUID moduleId, AssignmentCreateRequest request, User user) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(module.getCourse().getId(), user, true);

        Integer nextOrderIndex = assignmentRepository.findMaxOrderIndexByModuleId(moduleId) + 1;

        Assignment assignment = Assignment.builder()
                .module(module).title(request.getTitle())
                .description(request.getDescription())
                .maxScore(request.getMaxScore())
                .dueDate(request.getDueDate())
                .orderIndex(nextOrderIndex)
                .build();
        assignmentRepository.save(assignment);
    }

    @Transactional(readOnly = true)
    public List<AssignmentSummaryProjection> getAssignmentsByModule(UUID moduleId, User user) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        CourseRole userRole = securityValidator.getUserRoleInCourse(module.getCourse().getId(), user);

        if (userRole == CourseRole.TEACHER || userRole == CourseRole.ASSISTANT) {
            return assignmentRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
        } else {
            return assignmentRepository.findVisibleAssignmentsForStudent(moduleId);
        }
    }

    @Transactional
    public void updateAssignment(UUID assignmentId, AssignmentUpdateRequest request, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId)
                .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);

        assignment.setTitle(request.getTitle());
        assignment.setDescription(request.getDescription());
        assignment.setMaxScore(request.getMaxScore());
        assignment.setDueDate(request.getDueDate());

        assignmentRepository.save(assignment);
    }

    @Transactional
    public void deleteAssignment(UUID assignmentId, User user) {
        Assignment assignment = assignmentRepository.findByIdWithCourseContext(assignmentId)
                .orElseThrow(() -> new AppException("Assignment not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(assignment.getModule().getCourse().getId(), user, true);
        assignmentRepository.delete(assignment);
    }
}
