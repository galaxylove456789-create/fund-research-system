package com.fund.research.module.ai.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.ai.vo.RecommendFundVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecommendMapper {

    IPage<RecommendFundVO> selectRecommendFundPage(
            IPage<RecommendFundVO> page,
            @Param("userId") Long userId,
            @Param("scene") String scene
    );

    List<RecommendFundVO> selectRecommendFunds(
            @Param("userId") Long userId,
            @Param("scene") String scene,
            @Param("limit") Integer limit
    );

    RecommendFundVO selectLatestRecommendFund(
            @Param("userId") Long userId,
            @Param("fundId") Long fundId
    );
}