package com.fund.research.module.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

@Data
public class AiAnalyzeRequestDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String businessType = "FUND";

    private Long businessId;

    private String difyWorkflowId;

    @NotBlank(message = "query must not be blank")
    private String query;

    private Map<String, Object> inputs = new HashMap<>();
}
