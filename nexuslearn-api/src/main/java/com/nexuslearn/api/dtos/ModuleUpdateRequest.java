package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModuleUpdateRequest {
    @NotBlank(message = "Module title is required")
    private String title;
    private String description;
    @NotNull(message = "Publication status is required")
    private Boolean isPublished;
}