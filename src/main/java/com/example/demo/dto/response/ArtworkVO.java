package com.example.demo.dto.response;

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
}
