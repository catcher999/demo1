package com.example.demo.mapper.gallery;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.gallery.Category;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
}
