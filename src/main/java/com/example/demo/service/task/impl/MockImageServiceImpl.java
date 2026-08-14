package com.example.demo.service.task.impl;

import com.example.demo.service.task.ImageGenerationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Mock 图片生成实现
 * 通过 image.service=mock 激活
 * 返回一个占位图 URL，不调用真实服务
 */
@Service
@ConditionalOnProperty(name = "image.service", havingValue = "mock", matchIfMissing = true)
public class MockImageServiceImpl implements ImageGenerationService {

    @Override
    public String generate(String description) {
        // 模拟网络延迟，让状态流转更接近真实场景
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // 占位图：描述做简单 hash 后拼到 URL，便于区分不同任务
        int hash = Math.abs(description.hashCode());
        return "https://picsum.photos/seed/" + hash + "/512/512";
    }
}
