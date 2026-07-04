package com.fund.research.module.compare.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.compare.dto.CompareCreateDTO;
import com.fund.research.module.compare.vo.CompareDetailVO;
import com.fund.research.module.compare.vo.CompareRecordVO;

public interface CompareService {

    PageResult<CompareRecordVO> pageCompares(Long userId, Integer pageNo, Integer pageSize);

    CompareDetailVO createCompare(CompareCreateDTO request);

    CompareDetailVO getCompareDetail(Long compareId);

    Boolean deleteCompare(Long compareId, Long userId);
}
