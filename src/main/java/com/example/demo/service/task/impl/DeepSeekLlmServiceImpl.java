package com.example.demo.service.task.impl;

import com.example.demo.common.BusinessException;
import com.example.demo.config.LlmConfig;
import com.example.demo.service.task.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek LLM 实现
 * 调用 OpenAI 兼容接口 /v1/chat/completions
 * 30 秒超时由 LlmConfig 配置的 RestTemplate 保证
 * 通过 llm.provider=deepseek 激活（默认）
 */
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "deepseek", matchIfMissing = true)
@Slf4j
public class DeepSeekLlmServiceImpl implements LlmService {

    private static final String SYSTEM_PROMPT =
            "你是一个 AI 图像创作助手。根据用户描述生成详细的图像描述，用于指导图像生成。\n" +
            "\n" +
            "输出原则：\n" +
            "1. 优先突出用户最重视的元素\n" +
            "2. 补充画面必要细节\n" +
            "3. 保持用户表达的风格偏好\n" +
            "\n" +
            "输出格式（严格遵守）：\n" +
            "【标题】不超过10字的画面标题\n" +
            "---\n" +
            "（图像描述正文，适合结构化时按 主题/主体/场景/构图/色调/光影/风格 维度组织，不适合时用自然语言）\n" +
            "用户重点：（重申用户最在意的部分）";

    private final RestTemplate restTemplate;
    private final LlmConfig llmConfig;

    public DeepSeekLlmServiceImpl(@Qualifier("llmRestTemplate") RestTemplate restTemplate,
                                  LlmConfig llmConfig) {
        this.restTemplate = restTemplate;
        this.llmConfig = llmConfig;
    }

    @Override
    public String generateDescription(String prompt, String preference, List<String> history) {
        // 1. 组装 messages
        List<Map<String, String>> messages = new ArrayList<>();

        // 系统提示词
        messages.add(buildMessage("system", SYSTEM_PROMPT));

        // 会话级偏好（如果有）
        if (preference != null && !preference.isBlank()) {
            messages.add(buildMessage("system", "本会话用户偏好：" + preference));
        }

        // 上下文历史（按 [u1, a1, u2, a2] 顺序）
        if (history != null) {
            for (int i = 0; i < history.size(); i++) {
                String role = (i % 2 == 0) ? "user" : "assistant";
                messages.add(buildMessage(role, history.get(i)));
            }
        }

        // 当前 prompt
        messages.add(buildMessage("user", prompt));

        // 2. 组装请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);
        requestBody.put("max_tokens", 800);

        // 3. 组装请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        // 4. 调用 LLM API
        String url = llmConfig.getBaseUrl() + "/v1/chat/completions";
        try {
            Map<String, Object> response = restTemplate.postForObject(url, entity, Map.class);
            if (response == null) {
                throw new BusinessException("AI 服务返回空响应");
            }

            // OpenAI 格式：response.choices[0].message.content
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("AI 服务返回内容为空");
            }
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, String> message = (Map<String, String>) firstChoice.get("message");
            String content = message.get("content");

            log.info("LLM 生成成功，prompt={}, 内容长度={}", prompt, content.length());
            return content;

        } catch (RestClientException e) {
            log.error("LLM 调用失败: {}", e.getMessage());
            throw new BusinessException("AI 服务繁忙，请稍后重试");
        }
    }

    private Map<String, String> buildMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }
}
