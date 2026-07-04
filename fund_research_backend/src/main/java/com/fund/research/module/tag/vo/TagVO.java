package com.fund.research.module.tag.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "标签")
public class TagVO implements Serializable {

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

    @Schema(description = "标签说明")
    private String description;

    @Schema(description = "是否启用")
    private Integer enabled;

    @Schema(description = "关联基金数量")
    private Long fundCount;
}
