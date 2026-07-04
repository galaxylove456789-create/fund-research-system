package com.fund.research.module.fund.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Schema(description = "基金公告")
public class FundAnnouncementVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "公告 ID")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long announcementId;

    @Schema(description = "公告标题")
    private String title;

    @Schema(description = "公告日期")
    private LocalDate announcementDate;

    @Schema(description = "公告类别")
    private String category;

    @Schema(description = "来源链接")
    private String sourceUrl;

    @Schema(description = "摘要")
    private String summary;
}
