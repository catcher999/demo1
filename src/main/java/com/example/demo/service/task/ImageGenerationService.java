package com.example.demo.service.task;

/**
 * 图片生成服务接口（策略模式，可插拔）
 * 第一版用 Mock 实现，后续可替换为 Pollinations / Stable Diffusion 等
 */
public interface ImageGenerationService {

    /**
     * 根据描述生成图片
     * @param description AI 生成的结构化描述
     * @return 图片 URL
     */
    String generate(String description);
}
