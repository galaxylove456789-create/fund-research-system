package com.fund.research.module.portfolio.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.portfolio.dto.PortfolioCreateDTO;
import com.fund.research.module.portfolio.dto.PortfolioFromFilterDTO;
import com.fund.research.module.portfolio.dto.PortfolioFundAddDTO;
import com.fund.research.module.portfolio.dto.PortfolioUpdateDTO;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;

public interface PortfolioService {

    PageResult<PortfolioListVO> pagePortfolios(Long userId, String portfolioType, Integer pageNo, Integer pageSize);

    PortfolioDetailVO createPortfolio(PortfolioCreateDTO request);

    PortfolioDetailVO createFromFundFilter(PortfolioFromFilterDTO request);

    PortfolioDetailVO getPortfolioDetail(Long portfolioId);

    PortfolioDetailVO updatePortfolio(Long portfolioId, PortfolioUpdateDTO request);

    PortfolioDetailVO addFunds(Long portfolioId, PortfolioFundAddDTO request);

    PortfolioDetailVO removeFund(Long portfolioId, Long fundId);

    Boolean deletePortfolio(Long portfolioId);
}
