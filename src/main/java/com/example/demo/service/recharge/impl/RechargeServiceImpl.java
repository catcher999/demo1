package com.example.demo.service.recharge.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.common.BusinessException;
import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.recharge.CreateOrderRequest;
import com.example.demo.dto.recharge.PaymentResponse;
import com.example.demo.dto.recharge.RechargeOrderVO;
import com.example.demo.dto.recharge.RechargePackageVO;
import com.example.demo.entity.recharge.RechargeOrder;
import com.example.demo.entity.recharge.RechargePackage;
import com.example.demo.mapper.recharge.RechargeOrderMapper;
import com.example.demo.mapper.recharge.RechargePackageMapper;
import com.example.demo.service.points.PointsService;
import com.example.demo.service.recharge.AlipayService;
import com.example.demo.service.recharge.RechargeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Slf4j
public class RechargeServiceImpl implements RechargeService {

    private final RechargePackageMapper packageMapper;
    private final RechargeOrderMapper orderMapper;
    private final AlipayService alipayService;
    private final PointsService pointsService;
    private final RabbitTemplate rabbitTemplate;

    public RechargeServiceImpl(RechargePackageMapper packageMapper,
                                RechargeOrderMapper orderMapper,
                                AlipayService alipayService,
                                PointsService pointsService,
                                RabbitTemplate rabbitTemplate) {
        this.packageMapper = packageMapper;
        this.orderMapper = orderMapper;
        this.alipayService = alipayService;
        this.pointsService = pointsService;
        this.rabbitTemplate = rabbitTemplate;
    }

    // ==================== 套餐查询 ====================
    @Override
    public List<RechargePackageVO> listPackages() {
        LambdaQueryWrapper<RechargePackage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargePackage::getStatus, 1)
                .orderByAsc(RechargePackage::getId);
        List<RechargePackage> packages = packageMapper.selectList(wrapper);

        return packages.stream()
                .map(p -> new RechargePackageVO(
                        p.getId(), p.getName(), p.getPoints(),
                        p.getPrice(), p.getType()))
                .toList();
    }

    // ==================== 创建订单 ====================
    @Override
    public PaymentResponse createOrder(Long userId, CreateOrderRequest request) {
        // 1. 校验套餐
        RechargePackage pkg = packageMapper.selectById(request.getPackageId());
        if (pkg == null || pkg.getStatus() != 1) {
            throw new BusinessException("套餐不存在或已下架");
        }

        // 2. 只支持支付宝
        if (!"alipay".equals(request.getPayMethod())) {
            throw new BusinessException("暂只支持支付宝支付");
        }

        // 3. 生成订单号
        String orderNo = generateOrderNo(userId);

        // 4. 创建订单（pending）
        RechargeOrder order = new RechargeOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setPackageId(pkg.getId());
        order.setPackageName(pkg.getName());
        order.setPoints(pkg.getPoints());
        order.setAmount(pkg.getPrice());
        order.setPayMethod(request.getPayMethod());
        order.setStatus("pending");
        orderMapper.insert(order);

        // 5. 推送延迟消息（30 分钟后自动关单）
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                orderNo
        );
        log.info("订单创建成功，orderNo={}, 已推送 30 分钟关单延迟消息", orderNo);

        // 6. 调支付宝生成支付表单
        String payForm = alipayService.createPagePay(
                orderNo,
                pkg.getPrice().toPlainString(),
                pkg.getName()
        );

        return new PaymentResponse(orderNo, payForm);
    }

    // ==================== 支付宝异步回调 ====================
    @Override
    public String handleNotify(Map<String, String> params) {
        // 1. 验签
        if (!alipayService.verifyNotifySign(params)) {
            log.warn("支付宝回调验签失败，params={}", params);
            return "fail";
        }

        // 2. 取关键参数
        String orderNo = params.get("out_trade_no");
        String tradeNo = params.get("trade_no");
        String tradeStatus = params.get("trade_status");

        log.info("收到支付宝回调，orderNo={}, tradeNo={}, tradeStatus={}",
                orderNo, tradeNo, tradeStatus);

        // 3. 只处理交易成功的状态
        if (!"TRADE_SUCCESS".equals(tradeStatus) && !"TRADE_FINISHED".equals(tradeStatus)) {
            return "success"; // 非成功状态，直接返回 success 不重复通知
        }

        // 4. 查订单并处理（幂等）
        RechargeOrder order = getByOrderNo(orderNo);
        if (order == null) {
            log.warn("回调订单不存在，orderNo={}", orderNo);
            return "fail";
        }

        // 已支付（幂等：重复回调直接返回 success）
        if ("paid".equals(order.getStatus())) {
            log.info("订单已支付，重复回调忽略，orderNo={}", orderNo);
            return "success";
        }

        // 已关闭的订单不能再变 paid（边界情况：用户支付前订单刚好被关单）
        if ("closed".equals(order.getStatus())) {
            log.warn("订单已关闭，但收到支付回调，orderNo={}", orderNo);
            // 退款流程略，这里先记录日志
            return "success";
        }

        // 5. 更新订单状态 + 回填交易号
        orderMapper.update(null,
                new LambdaUpdateWrapper<RechargeOrder>()
                        .eq(RechargeOrder::getOrderNo, orderNo)
                        .eq(RechargeOrder::getStatus, "pending") // 乐观锁：只有 pending 才能变 paid
                        .set(RechargeOrder::getStatus, "paid")
                        .set(RechargeOrder::getTradeNo, tradeNo)
        );

        // 6. 加算力
        pointsService.refundPoints(order.getUserId(), order.getPoints());
        log.info("✅ 订单支付成功，已加算力，orderNo={}, userId={}, points={}",
                orderNo, order.getUserId(), order.getPoints());

        return "success";
    }

    // ==================== 订单查询 ====================
    @Override
    public RechargeOrderVO getOrder(Long userId, String orderNo) {
        RechargeOrder order = getByOrderNoAndUserId(orderNo, userId);
        return toVO(order);
    }

    @Override
    public IPage<RechargeOrderVO> listOrders(Long userId, int page, int size) {
        Page<RechargeOrder> pageObj = new Page<>(page, size);
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getUserId, userId)
                .orderByDesc(RechargeOrder::getCreatedAt);

        IPage<RechargeOrder> result = orderMapper.selectPage(pageObj, wrapper);

        List<RechargeOrderVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();
        Page<RechargeOrderVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(voList);
        return voPage;
    }

    // ==================== 掉单补偿 ====================
    @Override
    public boolean compensateOrder(String orderNo) {
        RechargeOrder order = getByOrderNo(orderNo);
        if (order == null || !"pending".equals(order.getStatus())) {
            return false;
        }

        // 查询支付宝状态
        String tradeStatus = alipayService.queryTradeStatus(orderNo);
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            // 补偿：更新状态 + 加算力
            orderMapper.update(null,
                    new LambdaUpdateWrapper<RechargeOrder>()
                            .eq(RechargeOrder::getOrderNo, orderNo)
                            .eq(RechargeOrder::getStatus, "pending")
                            .set(RechargeOrder::getStatus, "paid")
            );
            pointsService.refundPoints(order.getUserId(), order.getPoints());
            log.info("✅ 掉单补偿成功，orderNo={}", orderNo);
            return true;
        }
        return false;
    }

    // ==================== 关闭过期订单（MQ 消费者调用） ====================
    @Override
    public boolean closeExpiredOrder(String orderNo) {
        RechargeOrder order = getByOrderNo(orderNo);
        if (order == null) {
            log.warn("关闭订单时订单不存在，orderNo={}", orderNo);
            return false;
        }

        // 只有 pending 才关闭
        if (!"pending".equals(order.getStatus())) {
            log.info("订单非 pending 状态，无需关闭，orderNo={}, status={}",
                    orderNo, order.getStatus());
            return false;
        }

        orderMapper.update(null,
                new LambdaUpdateWrapper<RechargeOrder>()
                        .eq(RechargeOrder::getOrderNo, orderNo)
                        .eq(RechargeOrder::getStatus, "pending")
                        .set(RechargeOrder::getStatus, "closed")
        );
        log.info("订单已关闭（30 分钟未支付），orderNo={}", orderNo);
        return true;
    }

    // ==================== 内部方法 ====================

    @Override
    public RechargeOrder getByOrderNo(String orderNo) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo);
        return orderMapper.selectOne(wrapper);
    }

    /** 查询订单并校验归属权 */
    private RechargeOrder getByOrderNoAndUserId(String orderNo, Long userId) {
        LambdaQueryWrapper<RechargeOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RechargeOrder::getOrderNo, orderNo)
                .eq(RechargeOrder::getUserId, userId);
        RechargeOrder order = orderMapper.selectOne(wrapper);
        if (order == null) {
            throw new BusinessException("订单不存在或无权访问");
        }
        return order;
    }

    /**
     * 生成订单号：时间戳 + userId 后 4 位 + 4 位随机数
     * 示例：20260808215930 + 1234 + 5678
     */
    private String generateOrderNo(Long userId) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String userSuffix = String.format("%04d", userId % 10000);
        String random = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return timestamp + userSuffix + random;
    }

    private RechargeOrderVO toVO(RechargeOrder order) {
        return new RechargeOrderVO(
                order.getId(),
                order.getOrderNo(),
                order.getPackageId(),
                order.getPackageName(),
                order.getPoints(),
                order.getAmount(),
                order.getPayMethod(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
