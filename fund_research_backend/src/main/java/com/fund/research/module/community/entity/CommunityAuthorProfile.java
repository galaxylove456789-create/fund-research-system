package com.fund.research.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("community_author_profile")
public class CommunityAuthorProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "author_id", type = IdType.AUTO)
    private Long authorId;

    private Long userId;

    private String nickname;

    private String avatar;

    private String intro;

    private Integer articleCount;

    private Integer followerCount;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
