package com.fund.research.module.compare.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.compare.dto.CompareCreateDTO;
import com.fund.research.module.compare.entity.FundCompareItem;
import com.fund.research.module.compare.entity.FundCompareRecord;
import com.fund.research.module.compare.mapper.CompareItemMapper;
import com.fund.research.module.compare.mapper.CompareRecordMapper;
import com.fund.research.module.compare.service.CompareService;
import com.fund.research.module.compare.vo.CompareDetailVO;
import com.fund.research.module.compare.vo.CompareFundVO;
import com.fund.research.module.compare.vo.CompareRecordVO;
import com.fund.research.module.fund.mapper.FundMapper;
import com.fund.research.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CompareServiceImpl implements CompareService {

    private final CompareRecordMapper compareRecordMapper;
    private final CompareItemMapper compareItemMapper;
    private final UserMapper userMapper;
    private final FundMapper fundMapper;

    @Override
    public PageResult<CompareRecordVO> pageCompares(Long userId, Integer pageNo, Integer pageSize) {
        assertUserExists(userId);
        Page<CompareRecordVO> page = Page.of(normalizePage(pageNo), normalizeSize(pageSize));
        compareRecordMapper.selectComparePage(page, userId);
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompareDetailVO createCompare(CompareCreateDTO request) {
        assertUserExists(request.getUserId());
        Set<Long> fundIds = new LinkedHashSet<>(request.getFundIds());
        fundIds.removeIf(Objects::isNull);
        if (fundIds.size() < 2) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "At least two distinct funds are required");
        }
        if (fundIds.size() > 10) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "At most ten funds can be compared");
        }
        for (Long fundId : fundIds) {
            assertFundExists(fundId);
        }

        FundCompareRecord record = new FundCompareRecord();
        record.setUserId(request.getUserId());
        record.setCompareDimension(defaultText(request.getCompareDimension(), "SCORE,RETURN,RISK"));
        compareRecordMapper.insert(record);

        int order = 1;
        for (Long fundId : fundIds) {
            FundCompareItem item = new FundCompareItem();
            item.setCompareId(record.getCompareId());
            item.setFundId(fundId);
            item.setDisplayOrder(order++);
            compareItemMapper.insert(item);
        }

        CompareDetailVO detail = getCompareDetail(record.getCompareId());
        record.setResultSummary(buildSummary(detail.getFunds()));
        compareRecordMapper.updateById(record);
        return getCompareDetail(record.getCompareId());
    }

    @Override
    public CompareDetailVO getCompareDetail(Long compareId) {
        if (compareId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "compareId must not be null");
        }
        CompareDetailVO detail = compareRecordMapper.selectCompareDetail(compareId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Compare record does not exist");
        }
        detail.setFunds(compareRecordMapper.selectCompareFunds(compareId));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCompare(Long compareId, Long userId) {
        if (compareId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "compareId must not be null");
        }
        FundCompareRecord record = compareRecordMapper.selectById(compareId);
        if (record == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Compare record does not exist");
        }
        if (userId != null && !userId.equals(record.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Compare record does not belong to this user");
        }
        compareItemMapper.delete(new LambdaQueryWrapper<FundCompareItem>()
                .eq(FundCompareItem::getCompareId, compareId));
        return compareRecordMapper.deleteById(compareId) > 0;
    }

    private String buildSummary(List<CompareFundVO> funds) {
        if (funds == null || funds.isEmpty()) {
            return "No funds in this comparison.";
        }
        CompareFundVO bestScore = funds.stream()
                .filter(f -> f.getTotalScore() != null)
                .max(Comparator.comparing(CompareFundVO::getTotalScore))
                .orElse(null);
        CompareFundVO bestReturn = funds.stream()
                .filter(f -> f.getReturn1y() != null)
                .max(Comparator.comparing(CompareFundVO::getReturn1y))
                .orElse(null);
        StringBuilder summary = new StringBuilder("Compared ").append(funds.size()).append(" funds.");
        if (bestScore != null) {
            summary.append(" Highest score: ")
                    .append(bestScore.getFundName())
                    .append(" (")
                    .append(formatDecimal(bestScore.getTotalScore()))
                    .append(").");
        }
        if (bestReturn != null) {
            summary.append(" Highest 1Y return: ")
                    .append(bestReturn.getFundName())
                    .append(" (")
                    .append(formatDecimal(bestReturn.getReturn1y()))
                    .append("%).");
        }
        return summary.toString();
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

    private String formatDecimal(BigDecimal value) {
        return value == null ? "n/a" : value.stripTrailingZeros().toPlainString();
    }
}
