package com.fund.research.module.admin.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "导入异常记录")
public class ImportErrorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long errorId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long batchId;

    private Integer rowNo;

    private String fundCode;

    private String errorField;

    private String errorReason;

    private String suggestion;

    private String rawData;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}