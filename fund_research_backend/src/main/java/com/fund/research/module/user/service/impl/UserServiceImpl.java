package com.fund.research.module.user.service.impl;

import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.module.user.dto.PasswordChangeDTO;
import com.fund.research.module.user.dto.UserProfileUpdateDTO;
import com.fund.research.module.user.entity.FundUser;
import com.fund.research.module.user.mapper.UserMapper;
import com.fund.research.module.user.service.UserService;
import com.fund.research.module.user.vo.UserProfileVO;
import com.fund.research.security.PasswordPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        Long safeUserId = userId == null ? 1L : userId;
        FundUser user = userMapper.selectById(safeUserId);
        if (user == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "user not found");
        }
        return toVO(user);
    }

    @Override
    public UserProfileVO updateUserProfile(Long userId, UserProfileUpdateDTO request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "userId must not be null");
        }
        FundUser existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "user not found");
        }

        FundUser update = new FundUser();
        update.setUserId(userId);
        if (StringUtils.hasText(request.getUsername())) {
            update.setUsername(request.getUsername().trim());
        }
        if (StringUtils.hasText(request.getAvatar())) {
            update.setAvatar(request.getAvatar().trim());
        }
        update.setGender(trimToNull(request.getGender()));
        update.setLocationName(trimToNull(request.getLocationName()));
        update.setInvestYears(request.getInvestYears());
        update.setBirthday(request.getBirthday());
        update.setSignature(trimToNull(request.getSignature()));
        update.setRiskPreference(trimToNull(request.getRiskPreference()));
        update.setUpdatedTime(LocalDateTime.now());

        userMapper.updateById(update);
        return getUserProfile(userId);
    }

    @Override
    public Boolean changePassword(Long userId, PasswordChangeDTO request) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        FundUser existing = userMapper.selectById(userId);
        if (existing == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "user not found");
        }
        if (!StringUtils.hasText(request.getOldPassword()) || !passwordEncoder.matches(request.getOldPassword(), existing.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "当前密码不正确");
        }
        if (!StringUtils.hasText(request.getNewPassword()) || !request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "两次输入的新密码不一致");
        }
        if (passwordEncoder.matches(request.getNewPassword(), existing.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "新密码不能与当前密码相同");
        }
        PasswordPolicy.validate(existing.getUsername(), request.getNewPassword());
        String encoded = passwordEncoder.encode(request.getNewPassword());
        existing.setPasswordHash(encoded);

        FundUser update = new FundUser();
        update.setUserId(userId);
        update.setPasswordHash(encoded);
        update.setUpdatedTime(LocalDateTime.now());
        userMapper.updateById(update);
        return true;
    }

    private UserProfileVO toVO(FundUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(defaultString(user.getUsername(), "Iris0504"));
        vo.setRoleCode(defaultString(user.getRoleCode(), "USER"));
        vo.setRoleLabel("ADMIN".equalsIgnoreCase(vo.getRoleCode()) ? "管理员" : "普通用户");
        vo.setRiskPreference(normalizeRiskPreference(user.getRiskPreference()));
        vo.setAvatar(defaultString(user.getAvatar(), firstChar(vo.getUsername())));
        vo.setGender(user.getGender());
        vo.setLocationName(user.getLocationName());
        vo.setInvestYears(user.getInvestYears());
        vo.setBirthday(user.getBirthday());
        vo.setSignature(user.getSignature());
        vo.setStatus(user.getStatus());
        return vo;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String defaultString(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String firstChar(String value) {
        return StringUtils.hasText(value) ? value.substring(0, 1).toUpperCase() : "I";
    }

    private String normalizeRiskPreference(String value) {
        if (!StringUtils.hasText(value)) {
            return "平衡型";
        }
        String trimmed = value.trim();
        if ("CONSERVATIVE".equalsIgnoreCase(trimmed) || "LOW".equalsIgnoreCase(trimmed)) {
            return "稳健型";
        }
        if ("BALANCED".equalsIgnoreCase(trimmed) || "MEDIUM".equalsIgnoreCase(trimmed)) {
            return "平衡型";
        }
        if ("AGGRESSIVE".equalsIgnoreCase(trimmed) || "HIGH".equalsIgnoreCase(trimmed)) {
            return "进取型";
        }
        return trimmed;
    }
}
