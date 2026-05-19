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

}
