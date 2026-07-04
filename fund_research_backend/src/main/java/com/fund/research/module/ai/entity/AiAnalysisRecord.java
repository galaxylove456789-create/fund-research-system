package com.fund.research.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("ai_analysis_record")
public class AiAnalysisRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "analysis_id", type = IdType.AUTO)
    private Long analysisId;

    private Long userId;

    private String businessType;

    private Long businessId;

    private String difyWorkflowId;

    private String requestPayload;

    private String responseText;

    private String status;

    private String errorMessage;

    private LocalDateTime createdTime;
}
