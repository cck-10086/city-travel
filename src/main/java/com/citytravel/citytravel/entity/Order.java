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
@TableName("orders")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @NotBlank(message = "订单号不能为空")
    @Size(max = 50, message = "订单号长度不能超过50个字符")
    private String orderNo;
    
    @NotNull(message = "用户ID不能为空")
    private Long userId;
    
    @NotBlank(message = "商品类型不能为空")
    @Size(max = 20, message = "商品类型长度不能超过20个字符")
    private String productType;
    
    @NotNull(message = "商品ID不能为空")
    private Long productId;
    
    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    private String productName;
    
    @NotNull(message = "数量不能为空")
    @Positive(message = "数量必须为正数")
    private Integer quantity;
    
    @NotNull(message = "总价不能为空")
    private BigDecimal totalPrice;
    
    private String bookingDate;
    
    private String status;
    
    private LocalDateTime payTime;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
