package com.fund.research.module.auth.service;

import com.fund.research.module.auth.dto.LoginDTO;
import com.fund.research.module.auth.dto.RegisterDTO;
import com.fund.research.module.auth.vo.AuthUserVO;

public interface AuthService {

    AuthUserVO login(LoginDTO request);

    AuthUserVO register(RegisterDTO request);
}
