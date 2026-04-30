package com.nexuslearn.api.dtos;

import com.nexuslearn.api.models.CourseRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMemberToCourseRequest {
    @NotBlank
    @Email
    private String email;
    @NotNull(message = "Role is required")
    private CourseRole role;
}
