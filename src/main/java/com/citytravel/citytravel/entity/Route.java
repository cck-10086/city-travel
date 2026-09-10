package com.citytravel.citytravel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("route")
public class Route {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @NotBlank(message = "路线名称不能为空")
    @Size(max = 100, message = "路线名称长度不能超过100个字符")
    private String name;
    
    private String description;
    
    private String location;
    
    @NotNull(message = "价格不能为空")
    private BigDecimal price;
    
    private Integer days;
    
    private String image;
    
    private Integer status;
    
    private Long merchantId;
    
    private String merchantName;
    
    private Long categoryId;
    
    private String categoryName;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
