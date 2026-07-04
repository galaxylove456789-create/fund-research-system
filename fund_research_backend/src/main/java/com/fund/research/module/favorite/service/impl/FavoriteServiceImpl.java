package com.fund.research.module.favorite.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.favorite.dto.FavoriteCreateDTO;
import com.fund.research.module.favorite.dto.FavoriteUpdateDTO;
import com.fund.research.module.favorite.entity.UserFavorite;
import com.fund.research.module.favorite.mapper.FavoriteMapper;
import com.fund.research.module.favorite.service.FavoriteService;
import com.fund.research.module.favorite.vo.FavoriteVO;
import com.fund.research.module.fund.mapper.FundMapper;
import com.fund.research.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final UserMapper userMapper;
    private final FundMapper fundMapper;

    @Override
    public PageResult<FavoriteVO> pageFavorites(Long userId, String favoriteGroup, Integer pageNo, Integer pageSize) {
        assertUserExists(userId);
        Page<FavoriteVO> page = Page.of(normalizePage(pageNo), normalizeSize(pageSize));
        favoriteMapper.selectFavoritePage(page, userId, normalizeGroup(favoriteGroup));
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FavoriteVO addFavorite(FavoriteCreateDTO request) {
        assertUserExists(request.getUserId());
        assertFundExists(request.getFundId());
        String group = normalizeGroup(request.getFavoriteGroup());
        UserFavorite existing = favoriteMapper.selectOne(new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, request.getUserId())
                .eq(UserFavorite::getFundId, request.getFundId())
                .eq(UserFavorite::getFavoriteGroup, group));
        if (existing != null) {
            if (StringUtils.hasText(request.getRemark())) {
                existing.setRemark(request.getRemark().trim());
                favoriteMapper.updateById(existing);
            }
            return favoriteMapper.selectFavoriteById(existing.getFavoriteId());
        }

        UserFavorite favorite = new UserFavorite();
        favorite.setUserId(request.getUserId());
        favorite.setFundId(request.getFundId());
        favorite.setFavoriteGroup(group);
        favorite.setRemark(trimToNull(request.getRemark()));
        favoriteMapper.insert(favorite);
        return favoriteMapper.selectFavoriteById(favorite.getFavoriteId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FavoriteVO updateFavorite(Long favoriteId, FavoriteUpdateDTO request) {
        if (favoriteId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "favoriteId must not be null");
        }
        UserFavorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Favorite does not exist");
        }
        if (request != null && request.getUserId() != null && !request.getUserId().equals(favorite.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Favorite does not belong to this user");
        }
        if (request != null) {
            if (StringUtils.hasText(request.getFavoriteGroup())) {
                favorite.setFavoriteGroup(normalizeGroup(request.getFavoriteGroup()));
            }
            favorite.setRemark(trimToNull(request.getRemark()));
        }
        favoriteMapper.updateById(favorite);
        return favoriteMapper.selectFavoriteById(favoriteId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteFavorite(Long favoriteId, Long userId) {
        if (favoriteId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "favoriteId must not be null");
        }
        UserFavorite favorite = favoriteMapper.selectById(favoriteId);
        if (favorite == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Favorite does not exist");
        }
        if (userId != null && !userId.equals(favorite.getUserId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Favorite does not belong to this user");
        }
        return favoriteMapper.deleteById(favoriteId) > 0;
    }

    private void assertUserExists(Long userId) {
        if (userId == null || userMapper.selectById(userId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "User does not exist");
        }
    }

    private void assertFundExists(Long fundId) {
        if (fundId == null || fundMapper.selectById(fundId) == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "Fund does not exist");
        }
    }

    private long normalizePage(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo.longValue();
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100);
    }

    private String normalizeGroup(String favoriteGroup) {
        return StringUtils.hasText(favoriteGroup) ? favoriteGroup.trim() : "DEFAULT";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
