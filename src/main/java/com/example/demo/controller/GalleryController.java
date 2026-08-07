package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.response.ArtworkVO;
import com.example.demo.service.ArtworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private final ArtworkService artworkService;

    public GalleryController(ArtworkService artworkService) {
        this.artworkService = artworkService;
    }

    @GetMapping("/artworks")
    public ResponseEntity<?> getArtworks(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "heatScore") String sortBy
    ) {
        IPage<ArtworkVO> result = artworkService.getPublicArtworks(
                page,
                size,
                categoryId,
                sortBy
        );
        return ResponseEntity.ok(
                Result.success("Get artworks successful", result)
        );
    }
}
