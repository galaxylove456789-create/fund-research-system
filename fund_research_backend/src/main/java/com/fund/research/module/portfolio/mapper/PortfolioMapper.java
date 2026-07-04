package com.fund.research.module.portfolio.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.fund.dto.FundListQueryDTO;
import com.fund.research.module.portfolio.entity.FundPortfolio;
import com.fund.research.module.portfolio.vo.PortfolioDetailVO;
import com.fund.research.module.portfolio.vo.PortfolioFundVO;
import com.fund.research.module.portfolio.vo.PortfolioListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PortfolioMapper extends BaseMapper<FundPortfolio> {

    IPage<PortfolioListVO> selectPortfolioPage(
            IPage<PortfolioListVO> page,
            @Param("userId") Long userId,
            @Param("portfolioType") String portfolioType
    );

    PortfolioDetailVO selectPortfolioDetail(@Param("portfolioId") Long portfolioId);

    List<PortfolioFundVO> selectPortfolioFunds(@Param("portfolioId") Long portfolioId);

    BigDecimal selectLatestTotalScore(@Param("fundId") Long fundId);

    void refreshPortfolioStats(@Param("portfolioId") Long portfolioId);

    List<Long> selectFundIdsByQuery(@Param("query") FundListQueryDTO query, @Param("limit") Integer limit);
}
