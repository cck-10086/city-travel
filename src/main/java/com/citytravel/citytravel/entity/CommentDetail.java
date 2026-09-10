package com.citytravel.citytravel.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CommentDetail {
    private Long id;
    private Long strategyId;
    private String strategyTitle;
    private Long userId;
    private String userName;
    private String content;
    @JsonProperty("createdAt")
    private LocalDateTime createTime;
    private String type;
    private Long refId;
}