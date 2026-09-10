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
@TableName("attraction")
public class Attraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @NotBlank(message = "景点名称不能为空")
    @Size(max = 100, message = "景点名称长度不能超过100个字符")
    private String name;
    
    private String description;
    
    private String location;
    
    @NotNull(message = "价格不能为空")
    @Positive(message = "价格必须为正数")
    private BigDecimal price;
    
    private Integer stock;
    
    private String image;
    
    private Integer status;
    
    private Long merchantId;
    
    private String merchantName;
    
    private Long categoryId;
    
    private String categoryName;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
