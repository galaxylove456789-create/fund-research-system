package com.fund.research.module.manager.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.manager.dto.ManagerQueryDTO;
import com.fund.research.module.manager.vo.ManagerDetailVO;
import com.fund.research.module.manager.vo.ManagerListVO;

public interface ManagerService {

    PageResult<ManagerListVO> pageManagers(ManagerQueryDTO query);

    ManagerDetailVO getManagerDetail(Long managerId);

    PageResult<FundListVO> pageManagerFunds(Long managerId, Integer pageNo, Integer pageSize);
}
