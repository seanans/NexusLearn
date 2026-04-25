package com.nexuslearn.api.dtos;

import com.nexuslearn.api.models.CourseRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AddMemberToCourseRequest {
    @NotBlank
    @Email
    private String email;
    @NotNull(message = "Role is required")
    private CourseRole role;
}
