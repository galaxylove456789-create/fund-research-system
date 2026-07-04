package com.fund.research.module.community.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "社区作者")
public class AuthorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long authorId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String intro;

    private Integer articleCount;

    private Integer followerCount;

    private Boolean followed;
}