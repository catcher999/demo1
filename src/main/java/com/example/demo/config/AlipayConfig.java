package com.example.demo.config;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付宝沙箱支付配置
 *
 * 配置项前缀：alipay.*
 * 敏感字段（private-key、public-key）放在 application-local.properties
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {

    /** 沙箱 APPID */
    private String appId;

    /** 应用私钥（PKCS8 格式） */
    private String privateKey;

    /** 支付宝公钥 */
    private String publicKey;

    /** 网关地址（沙箱：https://openapi-sandbox.dl.alipaydev.com/gateway.do） */
    private String gateway;

    /** 异步回调地址（需公网可达） */
    private String notifyUrl;

    /** 同步返回地址（支付后跳回前端） */
    private String returnUrl;

    /** 签名类型 */
    private String signType = "RSA2";

    /** 字符编码 */
    private String charset = "UTF-8";

    /** 数据格式 */
    private String format = "json";

    /**
     * AlipayClient 单例
     * 使用证书模式时改用 CertAlipayRequest，这里用公钥模式（沙箱够用）
     */
    @Bean
    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                gateway,
                appId,
                privateKey,
                format,
                charset,
                publicKey,
                signType
        );
    }
}
