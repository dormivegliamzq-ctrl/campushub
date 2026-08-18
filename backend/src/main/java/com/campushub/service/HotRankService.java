package com.campushub.service;

import com.campushub.entity.Post;
import com.campushub.mapper.PostMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
/**
 * 帖子热度榜（Redis zset）
 *
 * key: post:hot    member: 帖子ID    score: 热度分
 * 热度分 = 点赞×3 + 评论×2 + 浏览×1
 *
 * 为什么用 zset 而不是 MySQL 排序：
 * 排行榜是"全局排序 + 高频读"场景。zset 基于跳表，插入 O(log N)、
 * 取 TopN 只需 O(log N + M)；而 MySQL 每次 ORDER BY like_count
 * 都要全表扫描 filesort，数据量大后不可接受。
 */
@Service
@RequiredArgsConstructor
public class HotRankService {

    public static final String HOT_KEY = "post:hot";

    private final PostMapper postMapper;
    private final StringRedisTemplate redisTemplate;

    /** 按帖子当前数据重算热度分并写入 zset（帖子已删除则移出榜单） */
    public void refreshScore(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            remove(postId);
            return;
        }
        double score = post.getLikeCount() * 3.0
                + post.getCommentCount() * 2.0
                + post.getViewCount();
        redisTemplate.opsForZSet().add(HOT_KEY, postId.toString(), score);
    }

    /** 从榜单移除 */
    public void remove(Long postId) {
        redisTemplate.opsForZSet().remove(HOT_KEY, postId.toString());
    }

    /**
     * 榜单为空时从 DB 全量重建（惰性兜底）。
     * zset 是内存数据：Redis 重启/被清空后榜单会丢失，
     * 下次读热门榜时发现 key 不存在就从数据库重建，保证榜单永远可恢复。
     */
    public void rebuildIfEmpty() {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(HOT_KEY))) {
            return;
        }
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getId)
                .last("LIMIT 500"));
        posts.forEach(p -> refreshScore(p.getId()));
    }
    public List<Long> topIds(int n) {
        Set<String> ids = redisTemplate.opsForZSet().reverseRange(HOT_KEY, 0, n - 1);
        return ids == null || ids.isEmpty()
                ? List.of()
                : ids.stream().map(Long::valueOf).toList();
    }
}
