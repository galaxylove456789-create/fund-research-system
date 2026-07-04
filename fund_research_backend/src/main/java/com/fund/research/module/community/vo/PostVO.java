package com.fund.research.module.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "社区帖子")
public class PostVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long postId;

    private Long authorId;

    private String author;

    private String authorNickname;

    private String authorAvatar;

    private String title;

    private String category;

    private Long relatedFundId;

    private String relatedFundCode;

    private String relatedFundName;

    private String summary;

    private String content;

    /** 标签列表（按英文逗号分隔解析为数组后传给前端）。 */
    private String tags;

    /** 标签数组，方便前端直接渲染。 */
    private java.util.List<String> tagList;

    private Integer viewCount;

    private Integer commentCount;

    private Integer likeCount;

    private Boolean liked;

    private Boolean viewed;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
