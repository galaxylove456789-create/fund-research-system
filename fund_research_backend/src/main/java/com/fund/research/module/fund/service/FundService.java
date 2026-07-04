package com.fund.research.module.fund.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.fund.dto.FundListQueryDTO;
import com.fund.research.module.fund.vo.FundAttributionVO;
import com.fund.research.module.fund.vo.FundDetailVO;
import com.fund.research.module.fund.vo.FundHoldingReportVO;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.fund.vo.FundMetricVO;
import com.fund.research.module.fund.vo.FundNavPointVO;
import com.fund.research.module.fund.vo.FundProfileVO;
import com.fund.research.module.fund.vo.FundTagVO;

import java.util.List;

/**
 * 基金查询服务。
 */
public interface FundService {

    /**
     * 基金分页查询。
     */
    PageResult<FundListVO> pageFunds(FundListQueryDTO query);

    /**
     * 基金详情。
     */
    FundDetailVO getFundDetail(Long fundId);

    /**
     * 基金画像。
     */
    FundProfileVO getFundProfile(Long fundId);

    /**
     * 基金净值曲线。
     */
    List<FundNavPointVO> getFundNav(Long fundId);

    /**
     * 基金标签。
     */
    List<FundTagVO> getFundTags(Long fundId);

    /**
     * 基金阶段指标。
     */
    List<FundMetricVO> getFundMetrics(Long fundId);
    List<FundHoldingReportVO> getFundHoldings(Long fundId);

    List<FundAttributionVO> getFundAttributions(Long fundId);
}
