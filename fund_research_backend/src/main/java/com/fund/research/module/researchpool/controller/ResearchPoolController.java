package com.fund.research.module.researchpool.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;
import com.fund.research.module.researchpool.dto.RecentViewCreateDTO;
import com.fund.research.module.researchpool.dto.SavedFilterCreateDTO;
import com.fund.research.module.researchpool.service.ResearchPoolService;
import com.fund.research.module.researchpool.vo.RecentViewVO;
import com.fund.research.module.researchpool.vo.SavedFilterVO;
import com.fund.research.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "我的研究池", description = "自选基金、模拟组合、保存筛选与最近浏览相关接口")
@RestController
@RequestMapping("/api/v1/my/research-pool")
@RequiredArgsConstructor
public class ResearchPoolController {

    private final ResearchPoolService researchPoolService;

    @GetMapping("/favorites")
    public Result<PageResult<?>> getFavorites(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        userId = resolveUserId(userId);
        return Result.success(researchPoolService.pageFavorites(userId, pageNo, pageSize));
    }

    @GetMapping("/portfolios")
    public Result<PageResult<PortfolioListVO>> getPortfolios(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        userId = resolveUserId(userId);
        return Result.success(researchPoolService.pagePortfolios(userId, pageNo, pageSize));
    }

    @GetMapping("/portfolios/{portfolioId}")
    public Result<PortfolioDetailVO> getPortfolioDetail(@PathVariable("portfolioId") Long portfolioId) {
        PortfolioDetailVO detail = researchPoolService.getPortfolioDetail(portfolioId);
        SecurityUtils.assertSelfOrAdmin(detail.getUserId());
        return Result.success(detail);
    }

    @GetMapping("/saved-filters")
    public Result<List<SavedFilterVO>> getSavedFilters(
            @RequestParam(value = "userId", required = false) Long userId) {
        userId = resolveUserId(userId);
        return Result.success(researchPoolService.listSavedFilters(userId));
    }

    @PostMapping("/saved-filters")
    public Result<SavedFilterVO> createSavedFilter(@Valid @RequestBody SavedFilterCreateDTO request) {
        SecurityUtils.assertSelfOrAdmin(request.getUserId());
        request.setUserId(resolveUserId(request.getUserId()));
        return Result.success(researchPoolService.createSavedFilter(request));
    }

    @DeleteMapping("/saved-filters/{filterId}")
    public Result<Boolean> deleteSavedFilter(
            @PathVariable("filterId") Long filterId,
            @RequestParam(value = "userId", required = false) Long userId) {
        userId = resolveUserId(userId);
        return Result.success(researchPoolService.deleteSavedFilter(filterId, userId));
    }

    @GetMapping("/recent-views")
    public Result<List<RecentViewVO>> getRecentViews(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        userId = resolveUserId(userId);
        return Result.success(researchPoolService.listRecentViews(userId, limit));
    }

    @PostMapping("/recent-views")
    public Result<RecentViewVO> recordRecentView(@Valid @RequestBody RecentViewCreateDTO request) {
        SecurityUtils.assertSelfOrAdmin(request.getUserId());
        request.setUserId(resolveUserId(request.getUserId()));
        return Result.success(researchPoolService.recordRecentView(request));
    }

    private Long resolveUserId(Long requestedUserId) {
        if (SecurityUtils.requireCurrentUser().isAdmin()) {
            return requestedUserId == null ? SecurityUtils.requireCurrentUserId() : requestedUserId;
        }
        if (requestedUserId != null) {
            SecurityUtils.assertSelfOrAdmin(requestedUserId);
        }
        return SecurityUtils.requireCurrentUserId();
    }
}
