package com.nexuslearn.api.services;

import com.nexuslearn.api.dtos.LessonCreateRequest;
import com.nexuslearn.api.dtos.LessonSummaryProjection;
import com.nexuslearn.api.exceptions.AppException;
import com.nexuslearn.api.models.Lesson;
import com.nexuslearn.api.models.Module;
import com.nexuslearn.api.models.User;
import com.nexuslearn.api.repositories.LessonRepository;
import com.nexuslearn.api.repositories.ModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LessonService {
    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final CourseSecurityValidator securityValidator;

    @Transactional
    public void createLesson(UUID moduleId, LessonCreateRequest request, User user) {
        Module module = moduleRepository.findById(moduleId).orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(module.getCourse().getId(), user, true);

        Integer nextOrderIndex = lessonRepository.findMaxOrderIndexByModuleId(moduleId) + 1;

        Lesson lesson = Lesson.builder().module(module).title(request.getTitle()).content(request.getContent()).videoUrl(request.getVideoUrl()).orderIndex(nextOrderIndex).build();

        lessonRepository.save(lesson);
    }

    @Transactional(readOnly = true)
    public List<LessonSummaryProjection> getLessonsByModule(UUID moduleId, User user) {
        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new AppException("Module not found", HttpStatus.NOT_FOUND));

        securityValidator.validateAccess(module.getCourse().getId(), user, false);

        return lessonRepository.findByModuleIdOrderByOrderIndexAsc(moduleId);
    }
}
