package com.fund.research.module.manager.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.fund.vo.FundListVO;
import com.fund.research.module.manager.dto.ManagerQueryDTO;
import com.fund.research.module.manager.vo.ManagerDetailVO;
import com.fund.research.module.manager.vo.ManagerListVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ManagerMapper {

    IPage<ManagerListVO> selectManagerPage(IPage<ManagerListVO> page, @Param("query") ManagerQueryDTO query);

    ManagerDetailVO selectManagerDetail(@Param("managerId") Long managerId);

    IPage<FundListVO> selectManagerFunds(IPage<FundListVO> page, @Param("managerId") Long managerId);
}
