package com.fund.research.module.favorite.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.favorite.dto.FavoriteCreateDTO;
import com.fund.research.module.favorite.dto.FavoriteUpdateDTO;
import com.fund.research.module.favorite.vo.FavoriteVO;

public interface FavoriteService {

    PageResult<FavoriteVO> pageFavorites(Long userId, String favoriteGroup, Integer pageNo, Integer pageSize);

    FavoriteVO addFavorite(FavoriteCreateDTO request);

    FavoriteVO updateFavorite(Long favoriteId, FavoriteUpdateDTO request);

    Boolean deleteFavorite(Long favoriteId, Long userId);
}
