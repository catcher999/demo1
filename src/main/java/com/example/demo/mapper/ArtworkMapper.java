package com.example.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.response.ArtworkVO;
import com.example.demo.entity.Artwork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArtworkMapper extends BaseMapper<Artwork> {

    IPage<ArtworkVO> selectPublicArtworks(
            IPage<ArtworkVO> page,
            @Param("categoryId") Long categoryId,
            @Param("sortBy") String sortBy
    );
}
