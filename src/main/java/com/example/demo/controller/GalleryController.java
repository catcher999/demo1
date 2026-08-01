package com.example.demo.controller;

import com.example.demo.common.Result;
import com.example.demo.service.impl.ArtworkServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private final ArtworkServiceImpl artworkServiceImpl;

    public GalleryController(ArtworkServiceImpl artworkServiceImpl) {
        this.artworkServiceImpl = artworkServiceImpl;
    }

    @GetMapping("/artworks")
    public ResponseEntity<?> getAllArtworks() {
        return ResponseEntity.ok(Result.success());
    }
    @GetMapping("/artworks/{id}")
    public ResponseEntity<?> getArtworkById(Long id) {
        return ResponseEntity.ok(Result.success());
    }
}
