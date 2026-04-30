package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LessonUpdateRequest {
    @NotBlank(message = "Lesson title is required")
    private String title;
    private String content;
    private String videoUrl;
    private Boolean isPublished = false;
    private LocalDateTime availableFrom;
}