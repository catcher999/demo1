package com.example.demo.service.task;

import java.util.List;

/**
 * 大语言模型服务接口（策略模式，可插拔）
 * 第一版用 DeepSeek 实现，后续可替换为通义千问 / 文心一言等
 */
public interface LlmService {

    /**
     * 调用 LLM 生成图像描述
     * @param prompt        当前用户输入
     * @param preference    会话级风格偏好（可为空）
     * @param history       上下文历史（同 session 下 succeeded 的任务，按时间顺序）
     *                      每个元素是一个 {user, assistant} 文本对，顺序为 [u1, a1, u2, a2, ...]
     * @return LLM 返回的原始文本（含【标题】行和描述正文）
     */
    String generateDescription(String prompt, String preference, List<String> history);
}
