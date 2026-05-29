package com.nexuslearn.api.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PendingAttachmentDto {
    private String fileUrl;
    private String fileName;
    private String fileType;
}