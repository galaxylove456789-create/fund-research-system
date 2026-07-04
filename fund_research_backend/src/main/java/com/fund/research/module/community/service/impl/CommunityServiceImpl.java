package com.fund.research.module.community.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fund.research.common.BusinessException;
import com.fund.research.common.ErrorCode;
import com.fund.research.common.PageResult;
import com.fund.research.module.community.entity.CommunityAuthorProfile;
import com.fund.research.module.community.mapper.CommunityMapper;
import com.fund.research.module.community.service.CommunityService;
import com.fund.research.module.community.vo.AuthorVO;
import com.fund.research.module.community.vo.CommentVO;
import com.fund.research.module.community.vo.PostVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final CommunityMapper communityMapper;

    @Override
    public PageResult<PostVO> pagePosts(Long authorId, String category, String keyword, String sort,
                                        Integer pageNo, Integer pageSize) {
        Page<PostVO> page = Page.of(normalizePageNo(pageNo), normalizePageSize(pageSize));
        communityMapper.selectPostPage(page, trimToNull(category), trimToNull(keyword), trimToNull(sort), authorId);
        enrichTags(page.getRecords());
        return PageResult.of(page);
    }

    @Override
    public PostVO getPostDetail(Long postId, Long currentUserId) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "postId must not be null");
        }
        PostVO vo = communityMapper.selectPostDetail(postId, currentUserId);
        if (vo == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "post not found");
        }
        vo.setLiked(Boolean.TRUE.equals(vo.getLiked()));
        vo.setViewed(Boolean.TRUE.equals(vo.getViewed()));
        enrichTags(Collections.singletonList(vo));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO recordPostView(Long postId, Long userId, String ipAddress, String userAgent) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "postId must not be null");
        }
        getPostDetail(postId, userId);
        String safeIp = StringUtils.hasText(ipAddress) ? ipAddress.trim() : null;
        String safeAgent = StringUtils.hasText(userAgent) ? userAgent.trim() : null;
        if (safeAgent != null && safeAgent.length() > 500) {
            safeAgent = safeAgent.substring(0, 500);
        }
        communityMapper.insertPostView(postId, userId, safeIp, safeAgent);
        communityMapper.increasePostViewCount(postId);
        return getPostDetail(postId, userId);
    }

    @Override
    public List<CommentVO> listPostComments(Long postId, Integer limit) {
        if (postId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "postId must not be null");
        }
        int safeLimit = limit == null || limit <= 0 ? 30 : Math.min(limit, 100);
        List<CommentVO> comments = communityMapper.selectPostComments(postId, safeLimit);
        return comments == null ? Collections.emptyList() : comments;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentVO createComment(Long postId, Long userId, String content) {
        if (postId == null || userId == null || !StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "postId, userId and content must not be empty");
        }
        getPostDetail(postId, userId);
        Map<String, Object> params = new java.util.HashMap<>();
        params.put("postId", postId);
        params.put("userId", userId);
        params.put("content", content.trim());
        communityMapper.insertPostComment(params);
        communityMapper.increasePostCommentCount(postId);

        Long commentId = asLong(params.get("commentId"));
        List<CommentVO> comments = communityMapper.selectPostComments(postId, 100);
        for (CommentVO comment : comments) {
            if (comment.getCommentId() != null && comment.getCommentId().equals(commentId)) {
                return comment;
            }
        }
        return comments.isEmpty() ? null : comments.get(comments.size() - 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO likePost(Long postId, Long userId) {
        if (postId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "postId and userId must not be null");
        }
        getPostDetail(postId, userId);
        Integer exists = communityMapper.countPostLike(postId, userId);
        if (exists == null || exists == 0) {
            communityMapper.insertPostLike(postId, userId);
            communityMapper.increasePostLikeCount(postId);
        }
        return getPostDetail(postId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO unlikePost(Long postId, Long userId) {
        if (postId == null || userId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "postId and userId must not be null");
        }
        Integer deleted = communityMapper.deletePostLike(postId, userId);
        if (deleted != null && deleted > 0) {
            communityMapper.decreasePostLikeCount(postId);
        }
        return getPostDetail(postId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PostVO createPost(Long authorId, String title, String category, Long relatedFundId,
                             String summary, String content, String tags, String status) {
        if (!StringUtils.hasText(title) || !StringUtils.hasText(content)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "title and content must not be empty");
        }
        if (authorId == null) {
            authorId = resolveDefaultAuthorId();
        } else {
            authorId = resolveAuthorIdByUserId(authorId);
        }
        String safeTitle = title.trim();
        String safeCategory = StringUtils.hasText(category) ? category.trim() : "基金分析";
        String safeSummary = StringUtils.hasText(summary) ? summary.trim() : safeTitle;
        String safeContent = content.trim();
        String safeTags = StringUtils.hasText(tags) ? tags.trim() : null;
        String safeStatus = "DRAFT".equalsIgnoreCase(status) ? "DRAFT" : "PUBLISHED";

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("authorId", authorId);
        params.put("title", safeTitle);
        params.put("category", safeCategory);
        params.put("relatedFundId", relatedFundId);
        params.put("summary", safeSummary);
        params.put("content", safeContent);
        params.put("tags", safeTags);
        params.put("status", safeStatus);

        communityMapper.insertPost(params);
        if ("PUBLISHED".equals(safeStatus)) {
            communityMapper.increaseAuthorArticleCount(authorId);
        }
        Long postId = asLong(params.get("postId"));
        return getPostDetail(postId, authorId);
    }

    @Override
    public PageResult<PostVO> pageMyPosts(Long authorId, Integer pageNo, Integer pageSize) {
        if (authorId == null) {
            authorId = resolveDefaultAuthorId();
        } else {
            authorId = resolveAuthorIdByUserId(authorId);
        }
        Page<PostVO> page = Page.of(normalizePageNo(pageNo), normalizePageSize(pageSize));
        communityMapper.selectPostsByAuthor(page, authorId);
        enrichTags(page.getRecords());
        return PageResult.of(page);
    }

    @Override
    public AuthorVO getAuthorDetail(Long authorId, Long currentUserId) {
        if (authorId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "authorId must not be null");
        }
        AuthorVO vo = communityMapper.selectAuthorDetail(authorId, currentUserId);
        if (vo == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "author not found");
        }
        vo.setFollowed(Boolean.TRUE.equals(vo.getFollowed()));
        return vo;
    }

    @Override
    public List<AuthorVO> listRecommendedAuthors(Long currentUserId, Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 8 : Math.min(limit, 50);
        List<AuthorVO> list = communityMapper.selectRecommendedAuthors(safeLimit, currentUserId);
        if (list == null) {
            return Collections.emptyList();
        }
        for (AuthorVO vo : list) {
            vo.setFollowed(Boolean.TRUE.equals(vo.getFollowed()));
        }
        return list;
    }

    @Override
    public List<AuthorVO> listFollowedAuthors(Long userId, Integer limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        List<AuthorVO> list = communityMapper.selectFollowedAuthors(userId, safeLimit);
        if (list == null) {
            return Collections.emptyList();
        }
        for (AuthorVO vo : list) {
            vo.setFollowed(Boolean.TRUE);
        }
        return list;
    }

    @Override
    public List<PostVO> listLikedPosts(Long userId, Integer limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        List<PostVO> list = communityMapper.selectLikedPosts(userId, safeLimit);
        enrichTags(list);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public List<Map<String, Object>> listMyComments(Long userId, Integer limit) {
        if (userId == null) {
            return Collections.emptyList();
        }
        int safeLimit = limit == null || limit <= 0 ? 50 : Math.min(limit, 100);
        List<Map<String, Object>> list = communityMapper.selectCommentsByUser(userId, safeLimit);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthorVO followAuthor(Long currentUserId, Long authorId) {
        if (currentUserId == null || authorId == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "userId and authorId must not be null");
        }
        AuthorVO author = getAuthorDetail(authorId, currentUserId);
        Integer exists = communityMapper.countAuthorFollow(currentUserId, authorId);
        if (exists == null || exists == 0) {
            communityMapper.insertAuthorFollow(currentUserId, authorId);
            communityMapper.increaseAuthorFollowerCount(authorId);
            author = getAuthorDetail(authorId, currentUserId);
        }
        author.setFollowed(Boolean.TRUE);
        return author;
    }

    @Override
    public List<String> listCommunityTags(Integer limit) {
        int safeLimit = limit == null || limit <= 0 ? 30 : Math.min(limit, 100);
        List<String> tags = communityMapper.selectCommunityTags(safeLimit);
        return tags == null ? Collections.emptyList() : tags;
    }

    @Override
    public List<String> listCommunityCategories() {
        List<String> cats = communityMapper.selectCommunityCategories();
        if (cats == null || cats.isEmpty()) {
            return Arrays.asList("全部", "基金分析", "筛选策略", "组合讨论", "风险提示", "新手提问", "系统公告");
        }
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        result.add("全部");
        result.addAll(cats);
        return result;
    }

    private void enrichTags(List<PostVO> records) {
        if (records == null) {
            return;
        }
        for (PostVO record : records) {
            if (record == null) {
                continue;
            }
            record.setAuthor(record.getAuthorNickname());
            String raw = record.getTags();
            if (StringUtils.hasText(raw)) {
                List<String> tagList = new java.util.ArrayList<>();
                for (String token : raw.split(",")) {
                    String trimmed = token == null ? "" : token.trim();
                    if (!trimmed.isEmpty()) {
                        tagList.add(trimmed);
                    }
                }
                record.setTagList(tagList);
            } else {
                record.setTagList(Collections.emptyList());
            }
        }
    }

    private Long resolveDefaultAuthorId() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityAuthorProfile> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.orderByAsc("author_id").last("LIMIT 1");
        CommunityAuthorProfile profile = communityMapper.selectOne(wrapper);
        if (profile == null) {
            throw new BusinessException(ErrorCode.DATA_NOT_EXISTS, "no community author profile exists");
        }
        return profile.getAuthorId();
    }

    private Long resolveAuthorIdByUserId(Long userId) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CommunityAuthorProfile> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        wrapper.eq("user_id", userId).last("LIMIT 1");
        CommunityAuthorProfile profile = communityMapper.selectOne(wrapper);
        if (profile != null) {
            return profile.getAuthorId();
        }
        communityMapper.insertAuthorProfileForUser(userId);
        CommunityAuthorProfile created = communityMapper.selectOne(wrapper);
        if (created != null) {
            return created.getAuthorId();
        }
        return resolveDefaultAuthorId();
    }

    private Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String && StringUtils.hasText((String) value)) {
            return Long.parseLong(((String) value).trim());
        }
        return null;
    }

    private long normalizePageNo(Integer pageNo) {
        return pageNo == null || pageNo < 1 ? 1L : pageNo.longValue();
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
