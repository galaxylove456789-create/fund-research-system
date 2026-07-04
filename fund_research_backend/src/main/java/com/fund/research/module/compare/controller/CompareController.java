package com.fund.research.module.compare.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.compare.dto.CompareCreateDTO;
import com.fund.research.module.compare.service.CompareService;
import com.fund.research.module.compare.vo.CompareDetailVO;
import com.fund.research.module.compare.vo.CompareRecordVO;
import com.fund.research.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/compares")
@RequiredArgsConstructor
public class CompareController {

    private final CompareService compareService;

    @GetMapping
    public Result<PageResult<CompareRecordVO>> pageCompares(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(compareService.pageCompares(userId, pageNo, pageSize));
    }

    @PostMapping
    public Result<CompareDetailVO> createCompare(@Valid @RequestBody CompareCreateDTO request) {
        SecurityUtils.assertSelfOrAdmin(request.getUserId());
        request.setUserId(SecurityUtils.requireCurrentUser().isAdmin() ? request.getUserId() : SecurityUtils.requireCurrentUserId());
        return Result.success(compareService.createCompare(request));
    }

    @GetMapping("/{compareId}")
    public Result<CompareDetailVO> getCompareDetail(@PathVariable("compareId") Long compareId) {
        return Result.success(compareService.getCompareDetail(compareId));
    }

    @DeleteMapping("/{compareId}")
    public Result<Boolean> deleteCompare(
            @PathVariable("compareId") Long compareId,
            @RequestParam(value = "userId", required = false) Long userId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (userId != null) {
            SecurityUtils.assertSelfOrAdmin(userId);
        }
        return Result.success(compareService.deleteCompare(compareId, SecurityUtils.requireCurrentUser().isAdmin() ? userId : currentUserId));
    }
}
