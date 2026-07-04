package com.fund.research.module.tag.controller;

import com.fund.research.common.Result;
import com.fund.research.module.tag.dto.TagQueryDTO;
import com.fund.research.module.tag.service.TagService;
import com.fund.research.module.tag.vo.TagVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "标签查询", description = "基金标签体系接口")
@RestController
@RequestMapping("/api/v1/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @Operation(summary = "标签列表", description = "查询可用于基金筛选的标签")
    @GetMapping
    public Result<List<TagVO>> listTags(@ParameterObject TagQueryDTO query) {
        return Result.success(tagService.listTags(query));
    }

    @Operation(summary = "标签类别", description = "查询标签类别列表")
    @GetMapping("/categories")
    public Result<List<String>> listCategories() {
        return Result.success(tagService.listCategories());
    }
}
