package com.fund.research.module.fund.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.fund.dto.FundListQueryDTO;
import com.fund.research.module.fund.mapper.FundMapper;
import com.fund.research.module.fund.service.FundService;
import com.fund.research.module.fund.vo.FundAttributionVO;
import com.fund.research.module.fund.vo.FundDetailVO;
import com.fund.research.module.fund.vo.FundHoldingReportVO;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.fund.vo.FundMetricVO;
import com.fund.research.module.fund.vo.FundNavPointVO;
import com.fund.research.module.fund.vo.FundProfileVO;
import com.fund.research.module.fund.vo.FundScoreVO;
import com.fund.research.module.fund.vo.FundTagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FundServiceImpl implements FundService {

    private final FundMapper fundMapper;

    @Override
    public PageResult<FundListVO> pageFunds(FundListQueryDTO query) {
        Page<FundListVO> page = Page.of(query.getPageNo(), query.getPageSize());
        fundMapper.selectFundPage(page, query);
        return PageResult.of(page);
    }

    @Override
    public FundDetailVO getFundDetail(Long fundId) {
        if (fundId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "fundId 不能为空");
        }
        FundDetailVO detail = fundMapper.selectFundDetail(fundId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "基金不存在");
        }
        return detail;
    }

    @Override
    public FundProfileVO getFundProfile(Long fundId) {
        FundDetailVO detail = getFundDetail(fundId);
        FundScoreVO score = fundMapper.selectLatestScore(fundId);

        FundProfileVO profile = new FundProfileVO();
        profile.setBasicInfo(detail);
        profile.setScore(score);
        profile.setTags(fundMapper.selectFundTags(fundId));
        profile.setNavChart(fundMapper.selectFundNav(fundId, 120));
        profile.setLatestMetrics(fundMapper.selectLatestMetrics(fundId));
        profile.setHoldingReports(loadHoldingReports(fundId, 4));
        profile.setAttributions(fundMapper.selectAttributions(fundId, 8));
        profile.setAnnouncements(fundMapper.selectAnnouncements(fundId, 10));
        return profile;
    }

    @Override
    public List<FundNavPointVO> getFundNav(Long fundId) {
        getFundDetail(fundId);
        return fundMapper.selectFundNav(fundId, 240);
    }

    @Override
    public List<FundTagVO> getFundTags(Long fundId) {
        getFundDetail(fundId);
        return fundMapper.selectFundTags(fundId);
    }

    @Override
    public List<FundMetricVO> getFundMetrics(Long fundId) {
        getFundDetail(fundId);
        return fundMapper.selectLatestMetrics(fundId);
    }
    @Override
    public List<FundHoldingReportVO> getFundHoldings(Long fundId) {
        getFundDetail(fundId);
        return loadHoldingReports(fundId, 8);
    }

    @Override
    public List<FundAttributionVO> getFundAttributions(Long fundId) {
        getFundDetail(fundId);
        return fundMapper.selectAttributions(fundId, 12);
    }

    private List<FundHoldingReportVO> loadHoldingReports(Long fundId, Integer limit) {
        List<FundHoldingReportVO> reports = fundMapper.selectHoldingReports(fundId, limit);
        for (FundHoldingReportVO report : reports) {
            report.setDetails(fundMapper.selectHoldingDetails(report.getReportId()));
        }
        return reports;
    }
}
