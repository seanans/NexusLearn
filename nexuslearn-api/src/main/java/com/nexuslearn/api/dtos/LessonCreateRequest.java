package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LessonCreateRequest {
    @NotBlank(message = "Lesson title is required")
    private String title;

    private String content;

    private String videoUrl;
}
