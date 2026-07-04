package com.fund.research.module.company.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.company.dto.CompanyQueryDTO;
import com.fund.research.module.company.mapper.CompanyMapper;
import com.fund.research.module.company.service.CompanyService;
import com.fund.research.module.company.vo.CompanyDetailVO;
import com.fund.research.module.company.vo.CompanyListVO;
import com.fund.research.module.fund.vo.FundListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyMapper companyMapper;

    @Override
    public PageResult<CompanyListVO> pageCompanies(CompanyQueryDTO query) {
        Page<CompanyListVO> page = Page.of(query.getPageNo(), query.getPageSize());
        companyMapper.selectCompanyPage(page, query);
        return PageResult.of(page);
    }

    @Override
    public CompanyDetailVO getCompanyDetail(Long companyId) {
        if (companyId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "companyId 不能为空");
        }
        CompanyDetailVO detail = companyMapper.selectCompanyDetail(companyId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "基金公司不存在");
        }
        return detail;
    }

    @Override
    public PageResult<FundListVO> pageCompanyFunds(Long companyId, Integer pageNo, Integer pageSize) {
        getCompanyDetail(companyId);
        long current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<FundListVO> page = Page.of(current, size);
        companyMapper.selectCompanyFunds(page, companyId);
        return PageResult.of(page);
    }
}
