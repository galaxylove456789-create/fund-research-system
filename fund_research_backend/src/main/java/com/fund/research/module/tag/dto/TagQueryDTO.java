package com.fund.research.module.tag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "标签查询参数")
public class TagQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "标签类别")
    private String category;

    @Schema(description = "关键字，匹配标签名称或编码")
    private String keyword;

    @Schema(description = "是否启用，默认只查启用标签")
    private Integer enabled = 1;
}
