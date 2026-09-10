package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Attraction;
import com.citytravel.citytravel.mapper.AttractionMapper;
import org.springframework.stereotype.Service;

@Service
public class AttractionService extends ServiceImpl<AttractionMapper, Attraction> {
}
