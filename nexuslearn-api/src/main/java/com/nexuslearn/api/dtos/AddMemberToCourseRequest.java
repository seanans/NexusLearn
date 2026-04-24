package com.nexuslearn.api.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
    @NotBlank(message = "Role is required")
    private String role;
}
