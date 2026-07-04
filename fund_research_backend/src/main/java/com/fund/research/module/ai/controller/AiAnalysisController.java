package com.fund.research.module.ai.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.ai.dto.AiAnalyzeRequestDTO;
import com.fund.research.module.ai.service.AiAnalysisService;
import com.fund.research.module.ai.vo.AiAnalysisRecordVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public Result<AiAnalysisRecordVO> analyze(@Valid @RequestBody AiAnalyzeRequestDTO request) {
        return Result.success(aiAnalysisService.analyze(request));
    }

    @GetMapping("/records")
    public Result<PageResult<AiAnalysisRecordVO>> pageRecords(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "businessType", required = false) String businessType,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        return Result.success(aiAnalysisService.pageRecords(userId, businessType, pageNo, pageSize));
    }

    @GetMapping("/records/{analysisId}")
    public Result<AiAnalysisRecordVO> getRecord(@PathVariable("analysisId") Long analysisId) {
        return Result.success(aiAnalysisService.getRecord(analysisId));
    }
}
