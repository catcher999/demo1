package com.example.demo.service.gallery;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.gallery.ArtworkVO;

public interface ArtworkService {

    IPage<ArtworkVO> getPublicArtworks(
            int page,
            int size,
            Long categoryId,
            String sortBy
    );

    /** 点赞（Redis 防重复 + 数据库原子更新热度） */
    void likeArtwork(Long userId, Long artworkId);

    /** 取消点赞 */
    void unlikeArtwork(Long userId, Long artworkId);

    /** 获取作品详情（含点赞状态 + 浏览数+1） */
    ArtworkVO getArtworkDetail(Long userId, Long artworkId);
}
