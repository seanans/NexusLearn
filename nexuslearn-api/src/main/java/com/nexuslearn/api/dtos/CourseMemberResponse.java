package com.nexuslearn.api.dtos;

import com.nexuslearn.api.models.CourseRole;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CourseMemberResponse {
    private String email;
    private String firstName;
    private String lastName;
    private CourseRole role;
    private LocalDateTime joinedAt;
}