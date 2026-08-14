package com.example.demo.service.recharge.impl;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.example.demo.common.BusinessException;
import com.example.demo.config.AlipayConfig;
import com.example.demo.service.recharge.AlipayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 支付宝沙箱实现
 *
 * 沙箱文档：https://opendocs.alipay.com/open/02nljt
 */
@Service
@Slf4j
public class AlipayServiceImpl implements AlipayService {

    private final AlipayClient alipayClient;
    private final AlipayConfig alipayConfig;

    public AlipayServiceImpl(AlipayClient alipayClient, AlipayConfig alipayConfig) {
        this.alipayClient = alipayClient;
        this.alipayConfig = alipayConfig;
    }

    @Override
    public String createPagePay(String orderNo, String amount, String subject) {
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        request.setReturnUrl(alipayConfig.getReturnUrl());

        // 业务参数
        request.setBizContent(
                "{\"out_trade_no\":\"" + orderNo + "\","
                + "\"total_amount\":\"" + amount + "\","
                + "\"subject\":\"" + subject + "\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\""
                + "}"
        );

        try {
            // 生成支付表单（自动提交的 HTML）
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            log.error("生成支付宝支付表单失败，orderNo={}", orderNo, e);
            throw new BusinessException("生成支付链接失败：" + e.getErrMsg());
        }
    }

    @Override
    public boolean verifyNotifySign(Map<String, String> params) {
        try {
            return AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType()
            );
        } catch (AlipayApiException e) {
            log.error("支付宝回调验签失败，params={}", params, e);
            return false;
        }
    }

    @Override
    public String queryTradeStatus(String orderNo) {
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        request.setBizContent("{\"out_trade_no\":\"" + orderNo + "\"}");

        try {
            AlipayTradeQueryResponse response = alipayClient.execute(request);
            if (response.isSuccess()) {
                return response.getTradeStatus();
            }
            log.warn("支付宝订单查询失败，orderNo={}, code={}, msg={}",
                    orderNo, response.getCode(), response.getMsg());
            return null;
        } catch (AlipayApiException e) {
            log.error("支付宝订单查询异常，orderNo={}", orderNo, e);
            return null;
        }
    }
}
