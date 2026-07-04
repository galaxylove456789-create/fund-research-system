package com.fund.research.module.company.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.company.dto.CompanyQueryDTO;
import com.fund.research.module.company.vo.CompanyDetailVO;
import com.fund.research.module.company.vo.CompanyListVO;
import com.fund.research.module.fund.vo.FundListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CompanyMapper {

    IPage<CompanyListVO> selectCompanyPage(IPage<CompanyListVO> page, @Param("query") CompanyQueryDTO query);

    CompanyDetailVO selectCompanyDetail(@Param("companyId") Long companyId);

    IPage<FundListVO> selectCompanyFunds(IPage<FundListVO> page, @Param("companyId") Long companyId);
}
