package com.nexuslearn.api.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageRequest {
    private String content;
    private String fileUrl;
}