package com.fund.research.module.company.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.company.dto.CompanyQueryDTO;
import com.fund.research.module.company.vo.CompanyDetailVO;
import com.fund.research.module.company.vo.CompanyListVO;
import com.fund.research.module.fund.vo.FundListVO;

public interface CompanyService {

    PageResult<CompanyListVO> pageCompanies(CompanyQueryDTO query);

    CompanyDetailVO getCompanyDetail(Long companyId);

    PageResult<FundListVO> pageCompanyFunds(Long companyId, Integer pageNo, Integer pageSize);
}
