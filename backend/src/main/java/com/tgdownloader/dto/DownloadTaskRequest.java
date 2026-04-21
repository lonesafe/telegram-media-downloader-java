package com.tgdownloader.dto;

import lombok.Data;

@Data
public class DownloadTaskRequest {

    private Long chatConfigId;
    private String chatLink;
    private Long startId;
    private Long endId;
    private Integer limit;
    private String filter;
    private String targetChatId;

    public DownloadTaskRequest() {}

    public DownloadTaskRequest(Long chatConfigId, String chatLink, Long startId, Long endId,
                               Integer limit, String filter, String targetChatId) {
        this.chatConfigId = chatConfigId;
        this.chatLink = chatLink;
        this.startId = startId;
        this.endId = endId;
        this.limit = limit;
        this.filter = filter;
        this.targetChatId = targetChatId;
    }

}
