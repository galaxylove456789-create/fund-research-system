package com.fund.research.module.researchpool.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;
import com.fund.research.module.researchpool.dto.RecentViewCreateDTO;
import com.fund.research.module.researchpool.dto.SavedFilterCreateDTO;
import com.fund.research.module.researchpool.vo.RecentViewVO;
import com.fund.research.module.researchpool.vo.SavedFilterVO;

import java.util.List;

public interface ResearchPoolService {

    PageResult<?> pageFavorites(Long userId, Integer pageNo, Integer pageSize);

    PageResult<PortfolioListVO> pagePortfolios(Long userId, Integer pageNo, Integer pageSize);

    List<SavedFilterVO> listSavedFilters(Long userId);

    SavedFilterVO createSavedFilter(SavedFilterCreateDTO request);

    Boolean deleteSavedFilter(Long filterId, Long userId);

    List<RecentViewVO> listRecentViews(Long userId, Integer limit);

    RecentViewVO recordRecentView(RecentViewCreateDTO request);

    PortfolioDetailVO getPortfolioDetail(Long portfolioId);
}
