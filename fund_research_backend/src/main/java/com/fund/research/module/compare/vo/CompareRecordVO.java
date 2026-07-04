package com.fund.research.module.compare.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class CompareRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long compareId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    private String compareDimension;

    private String resultSummary;

    private Integer fundCount;

    private LocalDateTime createdTime;
}
