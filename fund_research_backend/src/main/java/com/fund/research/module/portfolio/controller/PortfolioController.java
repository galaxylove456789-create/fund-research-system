package com.fund.research.module.portfolio.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.portfolio.dto.PortfolioCreateDTO;
import com.fund.research.module.portfolio.dto.PortfolioFromFilterDTO;
import com.fund.research.module.portfolio.dto.PortfolioFundAddDTO;
import com.fund.research.module.portfolio.dto.PortfolioUpdateDTO;
import com.fund.research.module.portfolio.service.PortfolioService;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;
import com.fund.research.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public Result<PageResult<PortfolioListVO>> pagePortfolios(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "portfolioType", required = false) String portfolioType,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        userId = resolveUserId(userId);
        return Result.success(portfolioService.pagePortfolios(userId, portfolioType, pageNo, pageSize));
    }

    @PostMapping
    public Result<PortfolioDetailVO> createPortfolio(@Valid @RequestBody PortfolioCreateDTO request) {
        SecurityUtils.assertSelfOrAdmin(request.getUserId());
        request.setUserId(resolveUserId(request.getUserId()));
        return Result.success(portfolioService.createPortfolio(request));
    }

    @PostMapping("/from-fund-filter")
    public Result<PortfolioDetailVO> createFromFundFilter(@Valid @RequestBody PortfolioFromFilterDTO request) {
        SecurityUtils.assertSelfOrAdmin(request.getUserId());
        request.setUserId(resolveUserId(request.getUserId()));
        return Result.success(portfolioService.createFromFundFilter(request));
    }

    @GetMapping("/{portfolioId}")
    public Result<PortfolioDetailVO> getPortfolioDetail(@PathVariable("portfolioId") Long portfolioId) {
        PortfolioDetailVO detail = portfolioService.getPortfolioDetail(portfolioId);
        SecurityUtils.assertSelfOrAdmin(detail.getUserId());
        return Result.success(detail);
    }

    @PutMapping("/{portfolioId}")
    public Result<PortfolioDetailVO> updatePortfolio(
            @PathVariable("portfolioId") Long portfolioId,
            @RequestBody PortfolioUpdateDTO request) {
        SecurityUtils.assertSelfOrAdmin(portfolioService.getPortfolioDetail(portfolioId).getUserId());
        return Result.success(portfolioService.updatePortfolio(portfolioId, request));
    }

    @PostMapping("/{portfolioId}/funds")
    public Result<PortfolioDetailVO> addFunds(
            @PathVariable("portfolioId") Long portfolioId,
            @Valid @RequestBody PortfolioFundAddDTO request) {
        SecurityUtils.assertSelfOrAdmin(portfolioService.getPortfolioDetail(portfolioId).getUserId());
        return Result.success(portfolioService.addFunds(portfolioId, request));
    }

    @DeleteMapping("/{portfolioId}/funds/{fundId}")
    public Result<PortfolioDetailVO> removeFund(
            @PathVariable("portfolioId") Long portfolioId,
            @PathVariable("fundId") Long fundId) {
        SecurityUtils.assertSelfOrAdmin(portfolioService.getPortfolioDetail(portfolioId).getUserId());
        return Result.success(portfolioService.removeFund(portfolioId, fundId));
    }

    @DeleteMapping("/{portfolioId}")
    public Result<Boolean> deletePortfolio(@PathVariable("portfolioId") Long portfolioId) {
        SecurityUtils.assertSelfOrAdmin(portfolioService.getPortfolioDetail(portfolioId).getUserId());
        return Result.success(portfolioService.deletePortfolio(portfolioId));
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
