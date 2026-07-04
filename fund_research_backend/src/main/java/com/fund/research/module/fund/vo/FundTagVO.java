package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "基金标签")
public class FundTagVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标签 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tagId;

    @Schema(description = "标签编码")
    private String tagCode;

    @Schema(description = "标签名称")
    private String tagName;

    @Schema(description = "标签类别")
    private String tagCategory;

    @Schema(description = "标签置信度")
    private BigDecimal confidence;

    @Schema(description = "标签来源")
    private String source;

    @Schema(description = "标签日期")
    private LocalDate tagDate;
}
