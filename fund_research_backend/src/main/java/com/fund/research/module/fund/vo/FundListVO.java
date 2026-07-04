package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "基金列表项")
public class FundListVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long fundId;

    private String fundCode;

    private String fundName;

    private String fundType;

    private String riskLevel;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long companyId;

    private String companyName;

    private String managerName;

    private BigDecimal fundScale;

    private LocalDate establishDate;

    private BigDecimal return1y;

    private BigDecimal maxDrawdown;

    private BigDecimal volatility;

    private BigDecimal sharpeRatio;

    private BigDecimal totalScore;

    private String recommendLevel;

    private List<String> tags = new ArrayList<>();

    public void setTagText(String tagText) {
        this.tags = new ArrayList<>();
        if (tagText == null || tagText.isBlank()) {
            return;
        }
        for (String tag : tagText.split(",")) {
            String trimmed = tag == null ? "" : tag.trim();
            if (!trimmed.isEmpty()) {
                this.tags.add(trimmed);
            }
        }
    }
}
