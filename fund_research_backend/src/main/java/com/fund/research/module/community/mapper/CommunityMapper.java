package com.fund.research.module.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.fund.research.module.community.entity.CommunityAuthorProfile;
import com.fund.research.module.community.vo.AuthorVO;
import com.fund.research.module.community.vo.CommentVO;
import com.fund.research.module.community.vo.PostVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

@Mapper
public interface CommunityMapper extends BaseMapper<CommunityAuthorProfile> {

    IPage<PostVO> selectPostPage(IPage<PostVO> page,
                                 @Param("category") String category,
                                 @Param("keyword") String keyword,
                                 @Param("sort") String sort,
                                 @Param("authorId") Long authorId);

    IPage<PostVO> selectPostsByAuthor(IPage<PostVO> page, @Param("authorId") Long authorId);

    PostVO selectPostDetail(@Param("postId") Long postId,
                            @Param("currentUserId") Long currentUserId);

    List<CommentVO> selectPostComments(@Param("postId") Long postId,
                                       @Param("limit") Integer limit);

    Long insertPost(Map<String, Object> params);

    Integer insertAuthorProfileForUser(@Param("userId") Long userId);

    Integer increaseAuthorArticleCount(@Param("authorId") Long authorId);

    Integer insertPostView(@Param("postId") Long postId,
                           @Param("userId") Long userId,
                           @Param("ipAddress") String ipAddress,
                           @Param("userAgent") String userAgent);

    Integer increasePostViewCount(@Param("postId") Long postId);

    Integer countPostLike(@Param("postId") Long postId,
                          @Param("userId") Long userId);

    Integer insertPostLike(@Param("postId") Long postId,
                           @Param("userId") Long userId);

    Integer deletePostLike(@Param("postId") Long postId,
                           @Param("userId") Long userId);

    Integer increasePostLikeCount(@Param("postId") Long postId);

    Integer decreasePostLikeCount(@Param("postId") Long postId);

    Integer insertPostComment(Map<String, Object> params);

    Integer increasePostCommentCount(@Param("postId") Long postId);

    AuthorVO selectAuthorDetail(@Param("authorId") Long authorId,
                                @Param("currentUserId") Long currentUserId);

    List<AuthorVO> selectRecommendedAuthors(@Param("limit") Integer limit,
                                            @Param("currentUserId") Long currentUserId);

    List<AuthorVO> selectFollowedAuthors(@Param("userId") Long userId,
                                         @Param("limit") Integer limit);

    List<PostVO> selectLikedPosts(@Param("userId") Long userId,
                                  @Param("limit") Integer limit);

    List<Map<String, Object>> selectCommentsByUser(@Param("userId") Long userId,
                                                   @Param("limit") Integer limit);

    Integer countAuthorFollow(@Param("userId") Long userId,
                              @Param("authorId") Long authorId);

    Integer insertAuthorFollow(@Param("userId") Long userId,
                               @Param("authorId") Long authorId);

    Integer increaseAuthorFollowerCount(@Param("authorId") Long authorId);

    List<String> selectCommunityTags(@Param("limit") Integer limit);

    List<String> selectCommunityCategories();
}
