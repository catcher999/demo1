package com.example.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("artwork")
public class Artwork {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String description;

    private String imageUrl;

    private String artist;

    private Date date;

    private Integer heatScore;

    private Long categoryId;

    private Boolean isPublic;

    private Long userId;
}
