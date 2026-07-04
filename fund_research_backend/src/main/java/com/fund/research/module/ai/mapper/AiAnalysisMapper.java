package com.fund.research.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.ai.entity.AiAnalysisRecord;
import com.fund.research.module.ai.vo.AiAnalysisRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAnalysisMapper extends BaseMapper<AiAnalysisRecord> {

    IPage<AiAnalysisRecordVO> selectAnalysisPage(
            IPage<AiAnalysisRecordVO> page,
            @Param("userId") Long userId,
            @Param("businessType") String businessType
    );

    AiAnalysisRecordVO selectAnalysisById(@Param("analysisId") Long analysisId);

    AiAnalysisRecordVO selectLatestSuccessfulAnalysis(
            @Param("userId") Long userId,
            @Param("businessType") String businessType,
            @Param("businessId") Long businessId
    );
}
