package com.citytravel.citytravel.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.citytravel.citytravel.entity.Comment;
import com.citytravel.citytravel.mapper.CommentMapper;
import org.springframework.stereotype.Service;

@Service
public class CommentService extends ServiceImpl<CommentMapper, Comment> {
}
