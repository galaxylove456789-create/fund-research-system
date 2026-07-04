package com.fund.research.module.ai.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.ai.dto.AiAnalyzeRequestDTO;
import com.fund.research.module.ai.vo.AiAnalysisRecordVO;
import com.fund.research.module.ai.vo.RecommendFundVO;

import java.util.List;

public interface AiAnalysisService {

    PageResult<AiAnalysisRecordVO> pageRecords(Long userId, String businessType, Integer pageNo, Integer pageSize);

    AiAnalysisRecordVO getRecord(Long analysisId);

    AiAnalysisRecordVO analyze(AiAnalyzeRequestDTO request);

    PageResult<RecommendFundVO> pageRecommendFunds(Long userId, String scene, Integer pageNo, Integer pageSize);

    List<RecommendFundVO> listRecommendFunds(Long userId, String scene, Integer limit);

    RecommendFundVO getLatestRecommendFund(Long userId, Long fundId);
}