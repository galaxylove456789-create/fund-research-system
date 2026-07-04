package com.fund.research.module.researchpool.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.fund.mapper.FundMapper;
import com.fund.research.module.favorite.service.FavoriteService;
import com.fund.research.module.portfolio.service.PortfolioService;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;
import com.fund.research.module.researchpool.dto.RecentViewCreateDTO;
import com.fund.research.module.researchpool.dto.SavedFilterCreateDTO;
import com.fund.research.module.researchpool.mapper.ResearchPoolMapper;
import com.fund.research.module.researchpool.service.ResearchPoolService;
import com.fund.research.module.researchpool.vo.RecentViewVO;
import com.fund.research.module.researchpool.vo.SavedFilterVO;
import com.fund.research.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ResearchPoolServiceImpl implements ResearchPoolService {

    private final ResearchPoolMapper researchPoolMapper;
    private final FavoriteService favoriteService;
    private final PortfolioService portfolioService;
    private final UserMapper userMapper;
    private final FundMapper fundMapper;

    @Override
    public PageResult<?> pageFavorites(Long userId, Integer pageNo, Integer pageSize) {
        return favoriteService.pageFavorites(userId, null, pageNo, pageSize);
    }

    @Override
    public PageResult<PortfolioListVO> pagePortfolios(Long userId, Integer pageNo, Integer pageSize) {
        return portfolioService.pagePortfolios(userId, null, pageNo, pageSize);
    }

    @Override
    public List<SavedFilterVO> listSavedFilters(Long userId) {
        List<SavedFilterVO> filters = researchPoolMapper.listSavedFilters(userId);
        return filters == null ? Collections.emptyList() : filters;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SavedFilterVO createSavedFilter(SavedFilterCreateDTO request) {
        assertUserExists(request.getUserId());
        researchPoolMapper.insertSavedFilter(request);
        List<SavedFilterVO> filters = researchPoolMapper.listSavedFilters(request.getUserId());
        return filters == null || filters.isEmpty() ? null : filters.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteSavedFilter(Long filterId, Long userId) {
        if (filterId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "filterId must not be null");
        }
        return researchPoolMapper.deleteSavedFilter(filterId, userId) > 0;
    }

    @Override
    public List<RecentViewVO> listRecentViews(Long userId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 20 : Math.min(limit, 100);
        List<RecentViewVO> list = researchPoolMapper.selectRecentViews(userId, safeLimit);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RecentViewVO recordRecentView(RecentViewCreateDTO request) {
        assertUserExists(request.getUserId());
        assertFundExists(request.getFundId());
        researchPoolMapper.upsertRecentView(request);
        List<RecentViewVO> list = listRecentViews(request.getUserId(), 1);
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    public PortfolioDetailVO getPortfolioDetail(Long portfolioId) {
        return portfolioService.getPortfolioDetail(portfolioId);
    }

    private void assertUserExists(Long userId) {
        if (userId == null || userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "User does not exist");
        }
    }

    private void assertFundExists(Long fundId) {
        if (fundId == null || fundMapper.selectById(fundId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Fund does not exist");
        }
    }
}
