package com.fund.research.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.module.auth.dto.LoginDTO;
import com.fund.research.module.auth.dto.RegisterDTO;
import com.fund.research.module.auth.service.AuthService;
import com.fund.research.module.auth.vo.AuthUserVO;
import com.fund.research.module.user.entity.FundUser;
import com.fund.research.module.user.mapper.UserMapper;
import com.fund.research.security.JwtTokenProvider;
import com.fund.research.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DEMO_HASH = "demo_hash";
    private static final String DEFAULT_USER_PASSWORD = "123456";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin123456";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public AuthUserVO login(LoginDTO request) {
        String account = trim(request.getAccount());
        FundUser user = userMapper.selectOne(new LambdaQueryWrapper<FundUser>()
                .eq(FundUser::getUsername, account)
                .last("limit 1"));
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码不正确");
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "当前账号已停用");
        }
        if (!passwordMatches(user, request.getPassword())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户名或密码不正确");
        }
        return toVO(user);
    }

    @Override
    public AuthUserVO register(RegisterDTO request) {
        String username = trim(request.getUsername());
        if (!StringUtils.hasText(username)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "用户名不能为空");
        }
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<FundUser>()
                .eq(FundUser::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "用户名已存在");
        }
        PasswordPolicy.validate(username, request.getPassword());

        FundUser user = new FundUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRoleCode("USER");
        user.setRiskPreference(normalizeRiskCode(request.getRiskPreference()));
        user.setAvatar(username.substring(0, 1).toUpperCase(Locale.ROOT));
        user.setSignature("正在使用 FundPilot 进行基金研究");
        user.setStatus(1);
        user.setCreatedTime(LocalDateTime.now());
        user.setUpdatedTime(LocalDateTime.now());
        userMapper.insert(user);
        return toVO(user);
    }

    private boolean passwordMatches(FundUser user, String rawPassword) {
        String passwordHash = user.getPasswordHash();
        if (!StringUtils.hasText(passwordHash)) {
            return false;
        }
        if (DEMO_HASH.equals(passwordHash)) {
            if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
                return DEFAULT_ADMIN_PASSWORD.equals(rawPassword);
            }
            return DEFAULT_USER_PASSWORD.equals(rawPassword);
        }
        return passwordEncoder.matches(rawPassword, passwordHash);
    }

    private AuthUserVO toVO(FundUser user) {
        AuthUserVO vo = new AuthUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRoleCode(StringUtils.hasText(user.getRoleCode()) ? user.getRoleCode() : "USER");
        vo.setRole(vo.getRoleCode().toLowerCase(Locale.ROOT));
        vo.setRiskPreference(normalizeRiskLabel(user.getRiskPreference()));
        vo.setAvatar(StringUtils.hasText(user.getAvatar()) ? user.getAvatar() : firstChar(user.getUsername()));
        vo.setGender(user.getGender());
        vo.setLocationName(user.getLocationName());
        vo.setInvestYears(user.getInvestYears());
        vo.setBirthday(user.getBirthday());
        vo.setSignature(user.getSignature());
        vo.setStatus(user.getStatus());
        vo.setToken(jwtTokenProvider.generateToken(user.getUserId(), user.getUsername(), vo.getRoleCode()));
        return vo;
    }

    private String normalizeRiskCode(String value) {
        String label = normalizeRiskLabel(value);
        if ("稳健型".equals(label)) {
            return "CONSERVATIVE";
        }
        if ("进取型".equals(label)) {
            return "AGGRESSIVE";
        }
        return "BALANCED";
    }

    private String normalizeRiskLabel(String value) {
        if (!StringUtils.hasText(value)) {
            return "平衡型";
        }
        String trimmed = value.trim();
        if ("CONSERVATIVE".equalsIgnoreCase(trimmed) || "LOW".equalsIgnoreCase(trimmed) || "稳健型".equals(trimmed)) {
            return "稳健型";
        }
        if ("AGGRESSIVE".equalsIgnoreCase(trimmed) || "HIGH".equalsIgnoreCase(trimmed) || "进取型".equals(trimmed)) {
            return "进取型";
        }
        return "平衡型";
    }

    private String firstChar(String value) {
        return StringUtils.hasText(value) ? value.substring(0, 1).toUpperCase(Locale.ROOT) : "U";
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
