package com.citytravel.citytravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("order_evaluation")
public class OrderEvaluation {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long orderId;
    
    private Long userId;
    
    private String content;
    
    private Integer rating;
    
    private LocalDateTime createTime;
}