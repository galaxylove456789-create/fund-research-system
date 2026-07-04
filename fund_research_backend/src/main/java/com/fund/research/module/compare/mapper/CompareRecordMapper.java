package com.fund.research.module.compare.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.compare.entity.FundCompareRecord;
import com.fund.research.module.compare.vo.CompareDetailVO;
import com.fund.research.module.compare.vo.CompareFundVO;
import com.fund.research.module.compare.vo.CompareRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CompareRecordMapper extends BaseMapper<FundCompareRecord> {

    IPage<CompareRecordVO> selectComparePage(IPage<CompareRecordVO> page, @Param("userId") Long userId);

    CompareDetailVO selectCompareDetail(@Param("compareId") Long compareId);

    List<CompareFundVO> selectCompareFunds(@Param("compareId") Long compareId);
}
