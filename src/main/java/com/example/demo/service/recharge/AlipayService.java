package com.example.demo.service.recharge;

import java.util.Map;

/**
 * 支付宝服务接口
 * 沙箱实现：AlipayServiceImpl
 *
 * 策略模式预留：后续可加 WechatPayService 等实现
 */
public interface AlipayService {

    /**
     * 生成电脑网站支付（PC 网页支付）的表单 HTML
     * @param orderNo  商户订单号
     * @param amount   支付金额（元）
     * @param subject  订单标题
     * @return 自动提交的 form HTML，前端直接写入页面即可跳转到支付宝
     */
    String createPagePay(String orderNo, String amount, String subject);

    /**
     * 验证支付宝异步回调签名
     * @param params 回调请求参数
     * @return true=验签通过
     */
    boolean verifyNotifySign(Map<String, String> params);

    /**
     * 主动查询支付宝订单状态（用于掉单补偿）
     * @param orderNo 商户订单号
     * @return 支付宝交易状态：WAIT_BUYER_PAY / TRADE_CLOSED / TRADE_SUCCESS / TRADE_FINISHED
     */
    String queryTradeStatus(String orderNo);
}
