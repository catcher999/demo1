package com.example.demo.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_task")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sessionId;

    private Long userId;

    private String prompt;

    private String description;

    private String status;

    private String resultUrl;

    private String errorMessage;

    private Long artworkId;

    /** 本次任务消耗的算力 */
    private Integer pointsCost;

    /** 使用的模型 */
    private String model;

    /** 推入MQ队列的时间 */
    private Date queuedAt;

    /** 任务完成时间（成功或失败） */
    private Date finishedAt;

    private Date createdAt;

    private Date updatedAt;
}
