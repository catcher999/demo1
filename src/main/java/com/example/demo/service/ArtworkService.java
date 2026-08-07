package com.example.demo.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.response.ArtworkVO;

public interface ArtworkService {

    IPage<ArtworkVO> getPublicArtworks(
            int page,
            int size,
            Long categoryId,
            String sortBy
    );
}
