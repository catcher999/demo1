package com.example.demo.service.recharge;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.demo.dto.recharge.CreateOrderRequest;
import com.example.demo.dto.recharge.PaymentResponse;
import com.example.demo.dto.recharge.RechargeOrderVO;
import com.example.demo.dto.recharge.RechargePackageVO;
import com.example.demo.entity.recharge.RechargeOrder;

import java.util.List;
import java.util.Map;

/**
 * 充值服务接口
 * 负责：套餐查询、订单创建、支付回调处理、订单查询
 */
public interface RechargeService {

    /** 查询已上架的充值套餐列表 */
    List<RechargePackageVO> listPackages();

    /**
     * 创建充值订单，并返回支付链接
     */
    PaymentResponse createOrder(Long userId, CreateOrderRequest request);

    /**
     * 处理支付宝异步回调
     * @param params 回调参数
     * @return "success" 表示处理成功（支付宝要求返回 success 字符串）
     */
    String handleNotify(Map<String, String> params);

    /** 查询订单详情（校验归属权） */
    RechargeOrderVO getOrder(Long userId, String orderNo);

    /** 我的订单列表（分页） */
    IPage<RechargeOrderVO> listOrders(Long userId, int page, int size);

    /**
     * 主动查询支付宝订单状态（掉单补偿）
     * @return true=已支付并补偿成功
     */
    boolean compensateOrder(String orderNo);

    /**
     * 关闭过期订单（MQ 消费者调用）
     * @return true=成功关闭
     */
    boolean closeExpiredOrder(String orderNo);

    /** 内部方法：根据订单号查询（不校验归属权，供 MQ 消费者使用） */
    RechargeOrder getByOrderNo(String orderNo);
}
