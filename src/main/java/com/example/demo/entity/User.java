package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user") // 对应数据库表名
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String password;

    private String email;

    // 可以根据需要添加更多字段
}
