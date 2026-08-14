package com.example.demo.mapper.task;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.demo.entity.task.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
