package com.fund.research.module.community.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("community_post")
public class CommunityPost implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "post_id", type = IdType.AUTO)
    private Long postId;

    private Long authorId;

    private String title;

    private String category;

    private Long relatedFundId;

    private String summary;

    private String content;

    private String tags;

    private Integer viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private String status;

    private LocalDateTime createdTime;

    private LocalDateTime updatedTime;
}
