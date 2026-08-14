package com.example.demo.service.mail;

public interface MailService {

    void sendVerificationCode(String email, String code);

    /** 任务结果通知（成功/失败） */
    void sendTaskResultNotify(String email, Long taskId, String status, String imageUrl);
}
