package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Favorite;
import com.citytravel.citytravel.mapper.FavoriteMapper;
import org.springframework.stereotype.Service;

@Service
public class FavoriteService extends ServiceImpl<FavoriteMapper, Favorite> {
}
