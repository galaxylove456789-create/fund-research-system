package com.fund.research.module.ai.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.ai.dto.AiAnalyzeRequestDTO;
import com.fund.research.module.ai.service.AiAnalysisService;
import com.fund.research.module.ai.vo.AiAnalysisRecordVO;
import com.fund.research.module.ai.vo.RecommendFundVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class RecommendController {

    private final AiAnalysisService aiAnalysisService;

    @GetMapping("/ai")
    public Result<List<RecommendFundVO>> getAiRecommendedFunds(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(aiAnalysisService.listRecommendFunds(userId, scene, limit));
    }

    @GetMapping("/ai/page")
    public Result<PageResult<RecommendFundVO>> pageAiRecommendedFunds(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(aiAnalysisService.pageRecommendFunds(userId, scene, pageNo, pageSize));
    }

    @GetMapping("/ai/list")
    public Result<List<RecommendFundVO>> listAiRecommendedFunds(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "scene", required = false) String scene,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(aiAnalysisService.listRecommendFunds(userId, scene, limit));
    }

    @GetMapping("/ai/funds/{fundId}")
    public Result<RecommendFundVO> getAiRecommendFund(
            @RequestParam(value = "userId", required = false) Long userId,
            @org.springframework.web.bind.annotation.PathVariable("fundId") Long fundId) {
        return Result.success(aiAnalysisService.getLatestRecommendFund(userId, fundId));
    }

    @PostMapping("/dify-explain")
    public Result<Map<String, Object>> difyExplain(@Valid @RequestBody AiAnalyzeRequestDTO request) {
        if (request.getBusinessType() == null) {
            request.setBusinessType("DIFY_EXPLAIN");
        }
        AiAnalysisRecordVO record = aiAnalysisService.analyze(request);
        Map<String, Object> response = new HashMap<>();
        response.put("workflow", "dify-fund-recommendation");
        response.put("analysisId", record.getAnalysisId());
        response.put("status", record.getStatus());
        response.put("explanation", record.getResponseText());
        response.put("errorMessage", record.getErrorMessage());
        response.put("request", record.getRequestPayload());
        return Result.success(response);
    }
}