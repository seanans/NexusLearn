package com.nexuslearn.api.dtos;

import java.util.List;
import java.util.UUID;

public record CourseSyllabusResponse(
        UUID courseId,
        List<SyllabusModuleDto> modules
) {
    // Nested Module Record
    public record SyllabusModuleDto(
            UUID moduleId,
            String title,
            Integer orderIndex,
            List<SyllabusItemDto> items
    ) {}

    // Nested Item Record
    public record SyllabusItemDto(
            UUID itemId,
            String title,
            ItemType type,
            Integer orderIndex
    ) {}
}