package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Attraction;
import com.citytravel.citytravel.mapper.AttractionMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttractionService extends ServiceImpl<AttractionMapper, Attraction> {

    /**
     * 原子扣减景点库存。
     *
     * @return true 扣减成功；false 库存不足或库存未启用
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deductStock(Long id, Integer quantity) {
        if (id == null || quantity == null || quantity < 1) {
            return false;
        }
        return baseMapper.deductStock(id, quantity) > 0;
    }

    /** 归还景点库存，订单取消时调用 */
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreStock(Long id, Integer quantity) {
        if (id == null || quantity == null || quantity < 1) {
            return false;
        }
        return baseMapper.restoreStock(id, quantity) > 0;
    }
}
