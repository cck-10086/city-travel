package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Homestay;
import com.citytravel.citytravel.mapper.HomestayMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomestayService extends ServiceImpl<HomestayMapper, Homestay> {

    /**
     * 原子扣减民宿库存。
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

    /** 归还民宿库存，订单取消时调用 */
    @Transactional(rollbackFor = Exception.class)
    public boolean restoreStock(Long id, Integer quantity) {
        if (id == null || quantity == null || quantity < 1) {
            return false;
        }
        return baseMapper.restoreStock(id, quantity) > 0;
    }
}
