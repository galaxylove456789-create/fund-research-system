package com.fund.research.module.manager.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@Schema(description = "基金经理查询参数")
public class ManagerQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "关键字，匹配经理姓名")
    private String keyword;

    @Schema(description = "基金公司 ID")
    private Long companyId;

    @Schema(description = "标签 ID 列表")
    private List<Long> tagIds;

    @Schema(description = "页码", defaultValue = "1")
    @Min(value = 1, message = "pageNo 必须 >= 1")
    private Integer pageNo = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    @Min(value = 1, message = "pageSize 必须 >= 1")
    @Max(value = 100, message = "pageSize 不能超过 100")
    private Integer pageSize = 10;

    public Integer getPageNo() {
        return pageNo == null || pageNo < 1 ? 1 : pageNo;
    }

    public Integer getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 100);
    }
}
