package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.common.Result;
import com.example.demo.dto.gallery.ArtworkVO;
import com.example.demo.service.gallery.ArtworkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /** 作品详情（需登录，含点赞状态 + 浏览数+1） */
    @GetMapping("/artworks/{id}")
    public ResponseEntity<?> getArtworkDetail(
            @PathVariable Long id,
            @RequestAttribute("currentUserId") Long userId
    ) {
        ArtworkVO result = artworkService.getArtworkDetail(userId, id);
        return ResponseEntity.ok(Result.success("Get artwork detail successful", result));
    }

    /** 点赞 */
    @PostMapping("/artworks/{id}/like")
    public ResponseEntity<?> likeArtwork(
            @PathVariable Long id,
            @RequestAttribute("currentUserId") Long userId
    ) {
        artworkService.likeArtwork(userId, id);
        return ResponseEntity.ok(Result.success("Like successful", null));
    }

    /** 取消点赞 */
    @DeleteMapping("/artworks/{id}/like")
    public ResponseEntity<?> unlikeArtwork(
            @PathVariable Long id,
            @RequestAttribute("currentUserId") Long userId
    ) {
        artworkService.unlikeArtwork(userId, id);
        return ResponseEntity.ok(Result.success("Unlike successful", null));
    }
}
