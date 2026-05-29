package com.nexuslearn.api.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ChatMessageRequest {
    private String content;
    private List<PendingAttachmentDto> attachments = new ArrayList<>();
}