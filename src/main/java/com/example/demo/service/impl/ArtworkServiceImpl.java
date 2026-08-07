package com.example.demo.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.dto.response.ArtworkVO;
import com.example.demo.mapper.ArtworkMapper;
import com.example.demo.service.ArtworkService;
import org.springframework.stereotype.Service;

@Service
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkMapper artworkMapper;

    public ArtworkServiceImpl(ArtworkMapper artworkMapper) {
        this.artworkMapper = artworkMapper;
    }

    @Override
    public IPage<ArtworkVO> getPublicArtworks(
            int page,
            int size,
            Long categoryId,
            String sortBy
    ) {
        Page<ArtworkVO> pageParam = new Page<>(page, size);
        return artworkMapper.selectPublicArtworks(
                pageParam,
                categoryId,
                sortBy
        );
    }
}
