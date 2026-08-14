package com.example.demo.service.mail.impl;

import com.example.demo.service.mail.MailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("AI创作平台 - 验证码");
        message.setText(
                "您的验证码是：" + code + "\n"
                + "验证码 5 分钟内有效，请勿泄露给他人。"
        );
        mailSender.send(message);
    }

    @Override
    public void sendTaskResultNotify(String email, Long taskId, String status, String imageUrl) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("AI创作平台 - 任务#" + taskId + " 结果通知");

        String text;
        if ("succeeded".equals(status)) {
            text = "您的任务 #" + taskId + " 已生成成功！\n"
                    + "图片地址：" + imageUrl + "\n\n"
                    + "请登录平台查看并发布到画廊。";
        } else {
            text = "您的任务 #" + taskId + " 生成失败。\n"
                    + "已退还消耗的算力，请稍后重试。\n\n"
                    + "如需帮助，请联系客服。";
        }
        message.setText(text);
        mailSender.send(message);
    }
}
