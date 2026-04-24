package com.nexuslearn.api.dtos;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class CourseResponse {
    private UUID id;
    private String title;
    private String description;
    private String creatorName;
}
