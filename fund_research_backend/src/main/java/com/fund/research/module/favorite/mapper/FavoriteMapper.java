package com.fund.research.module.favorite.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.favorite.entity.UserFavorite;
import com.fund.research.module.favorite.vo.FavoriteVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteMapper extends BaseMapper<UserFavorite> {

    IPage<FavoriteVO> selectFavoritePage(
            IPage<FavoriteVO> page,
            @Param("userId") Long userId,
            @Param("favoriteGroup") String favoriteGroup
    );

    FavoriteVO selectFavoriteById(@Param("favoriteId") Long favoriteId);
}
