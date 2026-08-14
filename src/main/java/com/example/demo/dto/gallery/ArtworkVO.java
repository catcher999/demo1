package com.example.demo.dto.gallery;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtworkVO {

    private Long id;

    private String title;

    private String description;

    private String imageUrl;

    private String artist;

    private Date date;

    private Integer heatScore;

    private String categoryName;

    /** 点赞数 */
    private Integer likes;

    /** 浏览数 */
    private Integer views;

    /** 当前用户是否已点赞（仅详情接口返回） */
    private Boolean isLiked;
}
