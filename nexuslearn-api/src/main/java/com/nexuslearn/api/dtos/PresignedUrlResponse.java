package com.nexuslearn.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PresignedUrlResponse {
    private String uploadUrl;
    private String fileUrl;
    private String objectName;
}