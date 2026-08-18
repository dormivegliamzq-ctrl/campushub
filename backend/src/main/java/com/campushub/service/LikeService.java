package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.common.BizException;
import com.campushub.common.ResultCode;
import com.campushub.common.UserContext;
import com.campushub.entity.Post;
import com.campushub.entity.PostLike;
import com.campushub.mapper.PostLikeMapper;
import com.campushub.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 点赞业务：防重复点赞的双保险设计
 *
 * 第一道防线（快速路径）：Redis Set 记录"谁赞过这个帖子"
 *   - key: post:like:{postId}，member: userId
 *   - 内存判断，O(1)，挡掉绝大多数重复请求，不碰数据库
 *
 * 第二道防线（最后防线）：post_like 表唯一索引 uk_post_user(post_id, user_id)
 *   - 即使 Redis 数据丢失/并发竞争，插入重复记录时数据库会拒绝（DuplicateKeyException）
 *   - 缓存校验不能替代 DB 约束：Redis 可能重启丢数据、可能有竞态窗口，
 *     而唯一索引是数据库的物理保证
 */
@Service
@RequiredArgsConstructor
public class LikeService {

    private static final String LIKE_KEY_PREFIX = "post:like:";

    private final PostMapper postMapper;
    private final PostLikeMapper postLikeMapper;
    private final StringRedisTemplate redisTemplate;
    private final HotRankService hotRankService;

    /** 点赞 */
    @Transactional
    public void like(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_EXIST);
        }
        Long userId = UserContext.getUserId();
        String key = LIKE_KEY_PREFIX + postId;

        // 第一道防线：Redis 已记录 → 直接拒绝
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, userId.toString()))) {
            throw new BizException(ResultCode.ALREADY_LIKED);
        }

        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        try {
            postLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            // 第二道防线：唯一索引兜底（Redis 丢数据或并发竞争时走到这里）
            throw new BizException(ResultCode.ALREADY_LIKED);
        }

        // 记录到 Redis + 点赞数 +1（同一事务内）
        redisTemplate.opsForSet().add(key, userId.toString());
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("like_count = like_count + 1"));
        // 点赞影响热度，刷新榜单
        hotRankService.refreshScore(postId);
    }

    /** 取消点赞 */
    @Transactional
    public void unlike(Long postId) {
        Long userId = UserContext.getUserId();
        String key = LIKE_KEY_PREFIX + postId;

        // 快速路径：Redis 里没有记录 → 大概率没赞过
        if (Boolean.FALSE.equals(redisTemplate.opsForSet().isMember(key, userId.toString()))) {
            throw new BizException(ResultCode.NOT_LIKED);
        }

        // DB 是裁决者：真删了记录才算取消成功
        int rows = postLikeMapper.delete(new LambdaQueryWrapper<PostLike>()
                .eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId));
        if (rows == 0) {
            throw new BizException(ResultCode.NOT_LIKED);
        }

        redisTemplate.opsForSet().remove(key, userId.toString());
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, postId)
                .setSql("like_count = IF(like_count > 0, like_count - 1, 0)"));
        hotRankService.refreshScore(postId);
    }

    /** 某人是否已点赞（供帖子详情返回"我是否已赞"） */
    public boolean hasLiked(Long postId, Long userId) {
        if (userId == null) {
            return false;
        }
        return Boolean.TRUE.equals(
                redisTemplate.opsForSet().isMember(LIKE_KEY_PREFIX + postId, userId.toString()));
    }
}
