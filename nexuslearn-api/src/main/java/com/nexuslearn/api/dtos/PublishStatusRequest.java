package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PublishStatusRequest {
    @NotNull(message = "Publication status is required")
    private Boolean isPublished;
}