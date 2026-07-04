package com.fund.research.module.favorite.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

@Data
public class FavoriteCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "userId must not be null")
    private Long userId;

    @NotNull(message = "fundId must not be null")
    private Long fundId;

    private String favoriteGroup = "DEFAULT";

    private String remark;
}
