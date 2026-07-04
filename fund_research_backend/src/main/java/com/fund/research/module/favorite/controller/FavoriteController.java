package com.fund.research.module.favorite.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.favorite.dto.FavoriteCreateDTO;
import com.fund.research.module.favorite.dto.FavoriteUpdateDTO;
import com.fund.research.module.favorite.service.FavoriteService;
import com.fund.research.module.favorite.vo.FavoriteVO;
import com.fund.research.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public Result<PageResult<FavoriteVO>> pageFavorites(
            @RequestParam("userId") Long userId,
            @RequestParam(value = "favoriteGroup", required = false) String favoriteGroup,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(favoriteService.pageFavorites(userId, favoriteGroup, pageNo, pageSize));
    }

    @PostMapping
    public Result<FavoriteVO> addFavorite(@Valid @RequestBody FavoriteCreateDTO request) {
        SecurityUtils.assertSelfOrAdmin(request.getUserId());
        request.setUserId(SecurityUtils.requireCurrentUser().isAdmin() ? request.getUserId() : SecurityUtils.requireCurrentUserId());
        return Result.success(favoriteService.addFavorite(request));
    }

    @PutMapping("/{favoriteId}")
    public Result<FavoriteVO> updateFavorite(
            @PathVariable("favoriteId") Long favoriteId,
            @RequestBody FavoriteUpdateDTO request) {
        request.setUserId(SecurityUtils.requireCurrentUserId());
        return Result.success(favoriteService.updateFavorite(favoriteId, request));
    }

    @PutMapping
    public Result<FavoriteVO> updateFavoriteCompat(@RequestBody FavoriteUpdateDTO request) {
        throw new com.fund.research.common.BusinessException(
                com.fund.research.common.ErrorCode.PARAM_INVALID,
                "favoriteId is required; use PUT /api/v1/favorites/{favoriteId}"
        );
    }

    @DeleteMapping("/{favoriteId}")
    public Result<Boolean> deleteFavorite(
            @PathVariable("favoriteId") Long favoriteId,
            @RequestParam(value = "userId", required = false) Long userId) {
        Long currentUserId = SecurityUtils.requireCurrentUserId();
        if (userId != null) {
            SecurityUtils.assertSelfOrAdmin(userId);
        }
        return Result.success(favoriteService.deleteFavorite(favoriteId, SecurityUtils.requireCurrentUser().isAdmin() ? userId : currentUserId));
    }
}
