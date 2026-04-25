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
    List<ModuleSummaryProjection> findByCourseIdOrderByOrderIndexAsc(UUID courseId);
    @Query("SELECT COALESCE(MAX(m.orderIndex), 0) FROM Module m WHERE m.course.id = :courseId")
    Integer findMaxOrderIndexByCourseId(@Param("courseId") UUID courseId);
}
