package com.fund.research.module.ai.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class AiAnalysisRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long analysisId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String businessType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long businessId;

    private String difyWorkflowId;

    private String requestPayload;

    private String responseText;

    private String status;

    private String errorMessage;

    private LocalDateTime createdTime;
}
