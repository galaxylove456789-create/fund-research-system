package com.fund.research.module.community.controller;

import com.fund.research.common.PageResult;
import com.fund.research.common.Result;
import com.fund.research.module.community.service.CommunityService;
import com.fund.research.module.community.vo.AuthorVO;
import com.fund.research.module.community.vo.CommentVO;
import com.fund.research.module.community.vo.PostVO;
import com.fund.research.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Tag(name = "社区", description = "社区帖子、作者、标签与分类相关接口")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    @GetMapping("/posts")
    public Result<PageResult<PostVO>> getPostPage(
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "userId", required = false) Long userId) {
        return Result.success(communityService.pagePosts(authorId, category, keyword, sort, pageNo, pageSize));
    }

    @GetMapping("/featured")
    public Result<List<PostVO>> getFeaturedPosts(
            @RequestParam(value = "limit", required = false) Integer limit) {
        PageResult<PostVO> page = communityService.pagePosts(null, null, null, "featured", 1, limit == null ? 6 : limit);
        return Result.success(page.getRecords());
    }

    @GetMapping("/posts/{postId}")
    public Result<PostVO> getPostDetail(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "userId", required = false) Long userId) {
        userId = SecurityUtils.currentUserIdOrNull();
        return Result.success(communityService.getPostDetail(postId, userId));
    }

    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommentVO>> getPostComments(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(communityService.listPostComments(postId, limit));
    }

    @PostMapping("/posts/{postId}/view")
    public Result<PostVO> recordPostView(
            @PathVariable("postId") Long postId,
            @RequestParam(value = "userId", required = false) Long userId,
            HttpServletRequest request) {
        userId = SecurityUtils.currentUserIdOrNull();
        return Result.success(communityService.recordPostView(
                postId,
                userId,
                request.getRemoteAddr(),
                request.getHeader("User-Agent")
        ));
    }

    @PostMapping("/posts/{postId}/comments")
    public Result<CommentVO> createComment(
            @PathVariable("postId") Long postId,
            @RequestBody Map<String, Object> request) {
        return Result.success(communityService.createComment(
                postId,
                SecurityUtils.requireCurrentUserId(),
                asString(request.get("content"))
        ));
    }

    @PostMapping("/posts/{postId}/like")
    public Result<PostVO> likePost(
            @PathVariable("postId") Long postId,
            @RequestParam("userId") Long userId) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(communityService.likePost(postId, SecurityUtils.requireCurrentUserId()));
    }

    @DeleteMapping("/posts/{postId}/like")
    public Result<PostVO> unlikePost(
            @PathVariable("postId") Long postId,
            @RequestParam("userId") Long userId) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(communityService.unlikePost(postId, SecurityUtils.requireCurrentUserId()));
    }

    @PostMapping("/posts")
    public Result<PostVO> createPost(@RequestBody Map<String, Object> request) {
        Long authorId = SecurityUtils.requireCurrentUserId();
        Object relatedFundObj = request.get("relatedFundId");
        Long relatedFundId = null;
        if (relatedFundObj instanceof Number) {
            relatedFundId = ((Number) relatedFundObj).longValue();
        } else if (relatedFundObj instanceof String) {
            try {
                relatedFundId = Long.parseLong(((String) relatedFundObj).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        Object tagsObj = request.get("tags");
        String tags;
        if (tagsObj instanceof List) {
            tags = String.join(",", ((List<?>) tagsObj).stream().map(String::valueOf).toList());
        } else if (tagsObj instanceof String) {
            tags = (String) tagsObj;
        } else {
            tags = null;
        }
        return Result.success(communityService.createPost(
                authorId,
                (String) request.get("title"),
                (String) request.get("category"),
                relatedFundId,
                (String) request.get("summary"),
                (String) request.get("content"),
                tags,
                asString(request.get("status"))
        ));
    }

    @GetMapping("/my-posts")
    public Result<PageResult<PostVO>> getMyPosts(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "pageNo", required = false) Integer pageNo,
            @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        userId = resolveUserId(userId);
        return Result.success(communityService.pageMyPosts(userId, pageNo, pageSize));
    }

    @GetMapping("/my-follows")
    public Result<List<AuthorVO>> getMyFollows(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        userId = resolveUserId(userId);
        return Result.success(communityService.listFollowedAuthors(userId, limit));
    }

    @GetMapping("/my-likes")
    public Result<List<PostVO>> getMyLikes(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        userId = resolveUserId(userId);
        return Result.success(communityService.listLikedPosts(userId, limit));
    }

    @GetMapping("/my-comments")
    public Result<List<Map<String, Object>>> getMyComments(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        userId = resolveUserId(userId);
        return Result.success(communityService.listMyComments(userId, limit));
    }

    @GetMapping("/authors")
    public Result<List<AuthorVO>> getRecommendedAuthors(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(communityService.listRecommendedAuthors(userId, limit));
    }

    @GetMapping("/authors/recommended")
    public Result<List<AuthorVO>> getRecommendedAuthorsAlias(
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(communityService.listRecommendedAuthors(userId, limit));
    }

    @GetMapping("/authors/{authorId}")
    public Result<AuthorVO> getAuthorDetail(
            @PathVariable("authorId") Long authorId,
            @RequestParam(value = "userId", required = false) Long userId) {
        userId = SecurityUtils.currentUserIdOrNull();
        return Result.success(communityService.getAuthorDetail(authorId, userId));
    }

    @PostMapping("/authors/{authorId}/follow")
    public Result<AuthorVO> followAuthor(
            @PathVariable("authorId") Long authorId,
            @RequestParam("userId") Long userId) {
        SecurityUtils.assertSelfOrAdmin(userId);
        return Result.success(communityService.followAuthor(SecurityUtils.requireCurrentUserId(), authorId));
    }

    private Long resolveUserId(Long requestedUserId) {
        if (SecurityUtils.requireCurrentUser().isAdmin()) {
            return requestedUserId == null ? SecurityUtils.requireCurrentUserId() : requestedUserId;
        }
        if (requestedUserId != null) {
            SecurityUtils.assertSelfOrAdmin(requestedUserId);
        }
        return SecurityUtils.requireCurrentUserId();
    }

    @GetMapping("/tags")
    public Result<List<String>> getCommunityTags(
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.success(communityService.listCommunityTags(limit));
    }

    @GetMapping("/categories")
    public Result<List<String>> getCommunityCategories() {
        return Result.success(communityService.listCommunityCategories());
    }

    private Long asLong(Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong(((String) value).trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
