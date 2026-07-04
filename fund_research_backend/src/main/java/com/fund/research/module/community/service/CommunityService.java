package com.fund.research.module.community.service;

import com.fund.research.common.PageResult;
import com.fund.research.module.community.vo.AuthorVO;
import com.fund.research.module.community.vo.CommentVO;
import com.fund.research.module.community.vo.PostVO;

import java.util.List;
import java.util.Map;

public interface CommunityService {

    PageResult<PostVO> pagePosts(Long authorId, String category, String keyword, String sort,
                                 Integer pageNo, Integer pageSize);

    PostVO getPostDetail(Long postId, Long currentUserId);

    PostVO recordPostView(Long postId, Long userId, String ipAddress, String userAgent);

    List<CommentVO> listPostComments(Long postId, Integer limit);

    CommentVO createComment(Long postId, Long userId, String content);

    PostVO likePost(Long postId, Long userId);

    PostVO unlikePost(Long postId, Long userId);

    PostVO createPost(Long authorId, String title, String category, Long relatedFundId,
                      String summary, String content, String tags, String status);

    PageResult<PostVO> pageMyPosts(Long authorId, Integer pageNo, Integer pageSize);

    AuthorVO getAuthorDetail(Long authorId, Long currentUserId);

    List<AuthorVO> listRecommendedAuthors(Long currentUserId, Integer limit);

    List<AuthorVO> listFollowedAuthors(Long userId, Integer limit);

    List<PostVO> listLikedPosts(Long userId, Integer limit);

    List<Map<String, Object>> listMyComments(Long userId, Integer limit);

    AuthorVO followAuthor(Long currentUserId, Long authorId);

    List<String> listCommunityTags(Integer limit);

    List<String> listCommunityCategories();
}
