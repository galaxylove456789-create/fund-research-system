package com.fund.research.module.manager.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.manager.dto.ManagerQueryDTO;
import com.fund.research.module.manager.mapper.ManagerMapper;
import com.fund.research.module.manager.service.ManagerService;
import com.fund.research.module.manager.vo.ManagerDetailVO;
import com.fund.research.module.manager.vo.ManagerListVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManagerServiceImpl implements ManagerService {

    private final ManagerMapper managerMapper;

    @Override
    public PageResult<ManagerListVO> pageManagers(ManagerQueryDTO query) {
        Page<ManagerListVO> page = Page.of(query.getPageNo(), query.getPageSize());
        managerMapper.selectManagerPage(page, query);
        return PageResult.of(page);
    }

    @Override
    public ManagerDetailVO getManagerDetail(Long managerId) {
        if (managerId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "managerId 不能为空");
        }
        ManagerDetailVO detail = managerMapper.selectManagerDetail(managerId);
        if (detail == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "基金经理不存在");
        }
        return detail;
    }

    @Override
    public PageResult<FundListVO> pageManagerFunds(Long managerId, Integer pageNo, Integer pageSize) {
        getManagerDetail(managerId);
        long current = pageNo == null || pageNo < 1 ? 1 : pageNo;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<FundListVO> page = Page.of(current, size);
        managerMapper.selectManagerFunds(page, managerId);
        return PageResult.of(page);
    }
}
