package com.fund.research.module.community.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Schema(description = "社区帖子评论")
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long commentId;

    private Long postId;

    private Long userId;

    private String username;

    private String content;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdTime;
}
