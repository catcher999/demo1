package com.example.demo.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.user.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
