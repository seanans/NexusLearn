package com.nexuslearn.api.repositories;

import com.nexuslearn.api.dtos.ModuleSummaryProjection;
import com.nexuslearn.api.models.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ModuleRepository extends JpaRepository<Module, UUID> {
    // return entities for the syllabus
    List<Module> findByCourseIdOrderByOrderIndexAsc(UUID courseId);
    List<Module> findByCourseIdAndIsPublishedTrueOrderByOrderIndexAsc(UUID courseId);

    // return projections
    List<ModuleSummaryProjection> findProjectedByCourseIdOrderByOrderIndexAsc(UUID courseId);
    List<ModuleSummaryProjection> findProjectedByCourseIdAndIsPublishedTrueOrderByOrderIndexAsc(UUID courseId);

    // utils
    @Query("SELECT COALESCE(MAX(m.orderIndex), 0) FROM Module m WHERE m.course.id = :courseId")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") UUID courseId);
}
