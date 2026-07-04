package com.fund.research.module.researchpool.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.researchpool.dto.RecentViewCreateDTO;
import com.fund.research.module.researchpool.dto.SavedFilterCreateDTO;
import com.fund.research.module.researchpool.vo.RecentViewVO;
import com.fund.research.module.researchpool.vo.SavedFilterVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ResearchPoolMapper {

    IPage<SavedFilterVO> selectSavedFilters(IPage<SavedFilterVO> page,
                                            @Param("userId") Long userId);

    List<SavedFilterVO> listSavedFilters(@Param("userId") Long userId);

    List<RecentViewVO> selectRecentViews(@Param("userId") Long userId,
                                         @Param("limit") Integer limit);

    int insertSavedFilter(@Param("request") SavedFilterCreateDTO request);

    int deleteSavedFilter(@Param("filterId") Long filterId,
                          @Param("userId") Long userId);

    int upsertRecentView(@Param("request") RecentViewCreateDTO request);
}
