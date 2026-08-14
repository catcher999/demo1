package com.example.demo.entity.task;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("ai_model")
public class AiModel {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模型标识（如 mock-image） */
    private String name;

    /** 显示名称 */
    private String displayName;

    /** 单次消耗算力 */
    private Integer pointsCost;

    /** 状态：1启用 0禁用 */
    private Integer status;
}
