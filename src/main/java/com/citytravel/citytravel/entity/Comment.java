package com.citytravel.citytravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@TableName("comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long strategyId;
    
    private Long userId;
    
    private String userName;
    
    @NotBlank(message = "评论内容不能为空")
    private String content;
    
    private LocalDateTime createTime;
    
    private String type;
    
    private Long refId;
    
    private Long parentId;
    
    private Long replyToId;
    
    private String replyToName;
    
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private List<Comment> children = new ArrayList<>();
}
