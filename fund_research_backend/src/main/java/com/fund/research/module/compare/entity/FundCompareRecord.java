package com.fund.research.module.compare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("fund_compare_record")
public class FundCompareRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "compare_id", type = IdType.AUTO)
    private Long compareId;

    private Long userId;

    private String compareDimension;

    private String resultSummary;

    private LocalDateTime createdTime;
}
