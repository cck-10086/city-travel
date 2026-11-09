package com.citytravel.citytravel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytravel.citytravel.entity.Homestay;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface HomestayMapper extends BaseMapper<Homestay> {

    /**
     * 原子扣减库存，语义同 AttractionMapper#deductStock。
     *
     * @return 1 成功，0 库存不足
     */
    @Update("UPDATE homestay SET stock = stock - #{quantity} "
            + "WHERE id = #{id} AND stock IS NOT NULL AND stock >= #{quantity}")
    int deductStock(@Param("id") Long id, @Param("quantity") Integer quantity);

    /** 归还库存 */
    @Update("UPDATE homestay SET stock = stock + #{quantity} WHERE id = #{id} AND stock IS NOT NULL")
    int restoreStock(@Param("id") Long id, @Param("quantity") Integer quantity);
}
