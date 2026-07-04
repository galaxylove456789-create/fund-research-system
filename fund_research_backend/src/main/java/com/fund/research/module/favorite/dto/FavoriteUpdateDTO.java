package com.fund.research.module.favorite.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class FavoriteUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String favoriteGroup;

    private String remark;
}
