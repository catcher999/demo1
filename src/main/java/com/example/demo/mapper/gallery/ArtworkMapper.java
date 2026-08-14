package com.example.demo.mapper.gallery;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.gallery.ArtworkVO;
import com.example.demo.entity.gallery.Artwork;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArtworkMapper extends BaseMapper<Artwork> {

    IPage<ArtworkVO> selectPublicArtworks(
            IPage<ArtworkVO> page,
            @Param("categoryId") Long categoryId,
            @Param("sortBy") String sortBy
    );

    /** 点赞：likes+1，重算 heat_score = likes×5 + views×1 */
    int incrementLike(@Param("id") Long id);

    /** 取消点赞：likes-1，重算 heat_score */
    int decrementLike(@Param("id") Long id);

    /** 增加浏览数 */
    int incrementView(@Param("id") Long id);
}
