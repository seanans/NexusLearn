package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseCreateRequest {
    @NotBlank(message = "Course title is required")
    private String title;

    private String description;
}
