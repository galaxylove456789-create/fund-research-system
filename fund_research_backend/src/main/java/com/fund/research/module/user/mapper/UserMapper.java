package com.fund.research.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fund.research.module.user.entity.FundUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<FundUser> {
}
