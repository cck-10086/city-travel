package com.citytravel.citytravel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.citytravel.citytravel.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
