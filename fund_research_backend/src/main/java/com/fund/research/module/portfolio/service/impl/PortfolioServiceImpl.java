package com.fund.research.module.portfolio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.fund.mapper.FundMapper;
import com.fund.research.module.portfolio.dto.PortfolioCreateDTO;
import com.fund.research.module.portfolio.dto.PortfolioFromFilterDTO;
import com.fund.research.module.portfolio.dto.PortfolioFundAddDTO;
import com.fund.research.module.portfolio.dto.PortfolioFundDTO;
import com.fund.research.module.portfolio.dto.PortfolioUpdateDTO;
import com.fund.research.module.portfolio.entity.FundPortfolio;
import com.fund.research.module.portfolio.entity.PortfolioFundRelation;
import com.fund.research.module.portfolio.mapper.PortfolioFundRelationMapper;
import com.fund.research.module.portfolio.mapper.PortfolioMapper;
import com.fund.research.module.portfolio.service.PortfolioService;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;
import com.fund.research.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioMapper portfolioMapper;
    private final PortfolioFundRelationMapper relationMapper;
    private final UserMapper userMapper;
    private final FundMapper fundMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<PortfolioListVO> pagePortfolios(Long userId, String portfolioType, Integer pageNo, Integer pageSize) {
        Page<PortfolioListVO> page = Page.of(normalizePage(pageNo), normalizeSize(pageSize));
        portfolioMapper.selectPortfolioPage(page, userId, trimToNull(portfolioType));
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioDetailVO createPortfolio(PortfolioCreateDTO request) {
        assertUserExists(request.getUserId());
        Map<Long, PortfolioFundDTO> funds = collectFunds(request.getFundIds(), request.getFunds());
        if (funds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "fundIds or funds must not be empty");
        }

        FundPortfolio portfolio = new FundPortfolio();
        portfolio.setUserId(request.getUserId());
        portfolio.setPortfolioName(request.getPortfolioName());
        portfolio.setPortfolioType(defaultText(request.getPortfolioType(), "RESEARCH_POOL"));
        portfolio.setSourceDimension(trimToNull(request.getSourceDimension()));
        portfolio.setSourceCondition(trimToNull(request.getSourceCondition()));
        portfolio.setDescription(trimToNull(request.getDescription()));
        portfolio.setTrackingEnabled(Boolean.FALSE.equals(request.getTrackingEnabled()) ? 0 : 1);
        portfolioMapper.insert(portfolio);

        insertRelations(portfolio.getPortfolioId(), funds);
        portfolioMapper.refreshPortfolioStats(portfolio.getPortfolioId());
        return getPortfolioDetail(portfolio.getPortfolioId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioDetailVO createFromFundFilter(PortfolioFromFilterDTO request) {
        assertUserExists(request.getUserId());
        int limit = request.getMaxFunds() == null ? 100 : Math.max(1, Math.min(request.getMaxFunds(), 200));
        List<Long> fundIds = portfolioMapper.selectFundIdsByQuery(request.getQuery(), limit);
        if (fundIds.isEmpty()) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "No funds matched the filter");
        }

        PortfolioCreateDTO create = new PortfolioCreateDTO();
        create.setUserId(request.getUserId());
        create.setPortfolioName(request.getPortfolioName());
        create.setPortfolioType(defaultText(request.getPortfolioType(), "RESEARCH_POOL"));
        create.setSourceDimension(defaultText(request.getSourceDimension(), "FUND_FILTER"));
        create.setDescription(request.getDescription());
        create.setFundIds(fundIds);
        try {
            create.setSourceCondition(objectMapper.writeValueAsString(request.getQuery()));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "Unable to serialize filter query");
        }
        return createPortfolio(create);
    }

    @Override
    public PortfolioDetailVO getPortfolioDetail(Long portfolioId) {
        if (portfolioId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "portfolioId must not be null");
        }
        PortfolioDetailVO detail = portfolioMapper.selectPortfolioDetail(portfolioId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Portfolio does not exist");
        }
        detail.setFunds(portfolioMapper.selectPortfolioFunds(portfolioId));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioDetailVO updatePortfolio(Long portfolioId, PortfolioUpdateDTO request) {
        getPortfolioDetail(portfolioId);
        FundPortfolio portfolio = new FundPortfolio();
        portfolio.setPortfolioId(portfolioId);
        portfolio.setPortfolioName(trimToNull(request.getPortfolioName()));
        portfolio.setPortfolioType(trimToNull(request.getPortfolioType()));
        portfolio.setSourceDimension(trimToNull(request.getSourceDimension()));
        portfolio.setSourceCondition(trimToNull(request.getSourceCondition()));
        portfolio.setDescription(trimToNull(request.getDescription()));
        if (request.getTrackingEnabled() != null) {
            portfolio.setTrackingEnabled(request.getTrackingEnabled() ? 1 : 0);
        }
        portfolio.setUpdatedTime(LocalDateTime.now());
        portfolioMapper.updateById(portfolio);
        return getPortfolioDetail(portfolioId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioDetailVO addFunds(Long portfolioId, PortfolioFundAddDTO request) {
        getPortfolioDetail(portfolioId);
        Map<Long, PortfolioFundDTO> funds = collectFunds(null, request.getFunds());
        if (funds.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "funds must not be empty");
        }
        insertRelations(portfolioId, funds);
        portfolioMapper.refreshPortfolioStats(portfolioId);
        return getPortfolioDetail(portfolioId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PortfolioDetailVO removeFund(Long portfolioId, Long fundId) {
        getPortfolioDetail(portfolioId);
        if (fundId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "fundId must not be null");
        }
        relationMapper.delete(new LambdaQueryWrapper<PortfolioFundRelation>()
                .eq(PortfolioFundRelation::getPortfolioId, portfolioId)
                .eq(PortfolioFundRelation::getFundId, fundId));
        portfolioMapper.refreshPortfolioStats(portfolioId);
        return getPortfolioDetail(portfolioId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deletePortfolio(Long portfolioId) {
        getPortfolioDetail(portfolioId);
        return portfolioMapper.deleteById(portfolioId) > 0;
    }

    private void insertRelations(Long portfolioId, Map<Long, PortfolioFundDTO> funds) {
        for (PortfolioFundDTO dto : funds.values()) {
            assertFundExists(dto.getFundId());
            relationMapper.delete(new LambdaQueryWrapper<PortfolioFundRelation>()
                    .eq(PortfolioFundRelation::getPortfolioId, portfolioId)
                    .eq(PortfolioFundRelation::getFundId, dto.getFundId()));
            PortfolioFundRelation relation = new PortfolioFundRelation();
            relation.setPortfolioId(portfolioId);
            relation.setFundId(dto.getFundId());
            relation.setWeight(dto.getWeight());
            relation.setAddSource(defaultText(dto.getAddSource(), "MANUAL"));
            relation.setSnapshotScore(portfolioMapper.selectLatestTotalScore(dto.getFundId()));
            relation.setRemark(trimToNull(dto.getRemark()));
            relationMapper.insert(relation);
        }
    }

    private Map<Long, PortfolioFundDTO> collectFunds(List<Long> fundIds, List<PortfolioFundDTO> fundDetails) {
        Map<Long, PortfolioFundDTO> funds = new LinkedHashMap<>();
        if (fundIds != null) {
            for (Long fundId : fundIds) {
                if (fundId != null && !funds.containsKey(fundId)) {
                    PortfolioFundDTO dto = new PortfolioFundDTO();
                    dto.setFundId(fundId);
                    dto.setAddSource("MANUAL");
                    funds.put(fundId, dto);
                }
            }
        }
        if (fundDetails != null) {
            for (PortfolioFundDTO dto : fundDetails) {
                if (dto != null && dto.getFundId() != null) {
                    funds.put(dto.getFundId(), dto);
                }
            }
        }
        return funds;
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

    private long normalizePage(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo.longValue();
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100);
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
