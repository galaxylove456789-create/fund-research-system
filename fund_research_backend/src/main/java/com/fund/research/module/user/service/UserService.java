package com.fund.research.module.user.service;

import com.fund.research.module.user.dto.UserProfileUpdateDTO;
import com.fund.research.module.user.dto.PasswordChangeDTO;
import com.fund.research.module.user.vo.UserProfileVO;

public interface UserService {

    UserProfileVO getUserProfile(Long userId);

    UserProfileVO updateUserProfile(Long userId, UserProfileUpdateDTO request);

    Boolean changePassword(Long userId, PasswordChangeDTO request);
}
