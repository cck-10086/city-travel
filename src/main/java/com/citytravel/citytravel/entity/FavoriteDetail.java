package com.citytravel.citytravel.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class FavoriteDetail {
    private Long id;
    private Long userId;
    private Long strategyId;
    private String title;
    private String content;
    private String authorName;
    private String image;
    private LocalDateTime createTime;
    private String type;
    private Long refId;
}