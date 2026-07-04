package com.fund.research.module.favorite.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("user_favorite")
public class UserFavorite implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "favorite_id", type = IdType.AUTO)
    private Long favoriteId;

    private Long userId;

    private Long fundId;

    private String favoriteGroup;

    private String remark;

    private LocalDateTime createdTime;
}
