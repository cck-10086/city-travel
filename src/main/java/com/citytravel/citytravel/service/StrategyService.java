package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Strategy;
import com.citytravel.citytravel.mapper.StrategyMapper;
import org.springframework.stereotype.Service;

@Service
public class StrategyService extends ServiceImpl<StrategyMapper, Strategy> {
    
    public void incrementViews(Long id) {
        Strategy strategy = getById(id);
        if (strategy != null) {
            strategy.setViews(strategy.getViews() + 1);
            updateById(strategy);
        }
    }
    
    public void incrementLikes(Long id) {
        Strategy strategy = getById(id);
        if (strategy != null) {
            strategy.setLikes(strategy.getLikes() + 1);
            updateById(strategy);
        }
    }
}
