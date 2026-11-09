package com.citytravel.citytravel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytravel.citytravel.entity.Attraction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AttractionMapper extends BaseMapper<Attraction> {

    /**
     * 原子扣减库存。WHERE 里带 stock >= quantity，靠数据库行锁保证不会超卖。
     *
     * @return 受影响行数，1 表示扣减成功，0 表示库存不足（并发下单时会被挡住）
     */
    @Update("UPDATE attraction SET stock = stock - #{quantity} "
            + "WHERE id = #{id} AND stock IS NOT NULL AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 归还库存（订单取消时调用） */
    @Update("UPDATE attraction SET stock = stock + #{quantity} WHERE id = #{id} AND stock IS NOT NULL")
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
