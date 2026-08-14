package com.example.demo.service.gallery.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.dto.gallery.ArtworkVO;
import com.example.demo.entity.gallery.Artwork;
import com.example.demo.entity.gallery.Category;
import com.example.demo.mapper.gallery.ArtworkMapper;
import com.example.demo.mapper.gallery.CategoryMapper;
import com.example.demo.service.gallery.ArtworkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class ArtworkServiceImpl implements ArtworkService {

    private final ArtworkMapper artworkMapper;
    private final CategoryMapper categoryMapper;
    private final StringRedisTemplate redisTemplate;

    /** 点赞关系 Key：like:{userId}:{artworkId} */
    private static final String LIKE_KEY = "like:";

    /** 浏览去重 Key：view:{userId}:{artworkId}，5 分钟内重复访问不计数 */
    private static final String VIEW_KEY = "view:";
    private static final Duration VIEW_DEDUP_TTL = Duration.ofMinutes(5);

    /** 点赞 key TTL：30 天后过期，用 DB likes 字段做最终一致性兜底 */
    private static final Duration LIKE_KEY_TTL = Duration.ofDays(30);

    public ArtworkServiceImpl(ArtworkMapper artworkMapper,
                               CategoryMapper categoryMapper,
                               StringRedisTemplate redisTemplate) {
        this.artworkMapper = artworkMapper;
        this.categoryMapper = categoryMapper;
        this.redisTemplate = redisTemplate;
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

    @Override
    public void likeArtwork(Long userId, Long artworkId) {
        // 1. 校验作品存在且公开
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null || !Boolean.TRUE.equals(artwork.getIsPublic())) {
            throw new BusinessException("作品不存在或已被私密");
        }

        // 2. Redis 防重复（SETNX：key 不存在才设置成功）
        String likeKey = LIKE_KEY + userId + ":" + artworkId;
        Boolean success = redisTemplate.opsForValue().setIfAbsent(likeKey, "1", LIKE_KEY_TTL);
        if (Boolean.FALSE.equals(success)) {
            throw new BusinessException("您已点赞过该作品");
        }

        // 3. 数据库：likes+1，重算热度
        artworkMapper.incrementLike(artworkId);
        log.info("用户 {} 点赞作品 {}", userId, artworkId);
    }

    @Override
    public void unlikeArtwork(Long userId, Long artworkId) {
        // 1. 校验作品存在
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException("作品不存在");
        }

        // 2. 检查 Redis 中是否有点赞记录
        String likeKey = LIKE_KEY + userId + ":" + artworkId;
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(likeKey))) {
            throw new BusinessException("您尚未点赞该作品");
        }

        // 3. 删除 Redis 记录
        redisTemplate.delete(likeKey);

        // 4. 数据库：likes-1，重算热度
        artworkMapper.decrementLike(artworkId);
        log.info("用户 {} 取消点赞作品 {}", userId, artworkId);
    }

    @Override
    public ArtworkVO getArtworkDetail(Long userId, Long artworkId) {
        // 1. 查作品
        Artwork artwork = artworkMapper.selectById(artworkId);
        if (artwork == null) {
            throw new BusinessException("作品不存在");
        }

        // 2. 浏览数防刷：同一用户 5 分钟内重复访问只计一次
        String viewKey = VIEW_KEY + userId + ":" + artworkId;
        Boolean firstView = redisTemplate.opsForValue()
                .setIfAbsent(viewKey, "1", VIEW_DEDUP_TTL);
        if (Boolean.TRUE.equals(firstView)) {
            artworkMapper.incrementView(artworkId);
        }
        artwork = artworkMapper.selectById(artworkId); // 重新查询获取最新值

        // 3. 查点赞状态
        String likeKey = LIKE_KEY + userId + ":" + artworkId;
        boolean isLiked = Boolean.TRUE.equals(redisTemplate.hasKey(likeKey));

        // 4. 查分类名
        String categoryName = null;
        if (artwork.getCategoryId() != null) {
            Category category = categoryMapper.selectById(artwork.getCategoryId());
            if (category != null) {
                categoryName = category.getName();
            }
        }

        // 5. 组装 VO
        ArtworkVO vo = new ArtworkVO();
        vo.setId(artwork.getId());
        vo.setTitle(artwork.getTitle());
        vo.setDescription(artwork.getDescription());
        vo.setImageUrl(artwork.getImageUrl());
        vo.setArtist(artwork.getArtist());
        vo.setDate(artwork.getDate());
        vo.setHeatScore(artwork.getHeatScore());
        vo.setCategoryName(categoryName);
        vo.setLikes(artwork.getLikes());
        vo.setViews(artwork.getViews());
        vo.setIsLiked(isLiked);

        return vo;
    }
}
