package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.BizException;
import com.campushub.common.ResultCode;
import com.campushub.common.UserContext;
import com.campushub.dto.CreatePostRequest;
import com.campushub.dto.UpdatePostRequest;
import com.campushub.entity.Post;
import com.campushub.entity.User;
import com.campushub.mapper.PostMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.vo.PostVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 帖子业务：发布 / 分页列表 / 详情（缓存）/ 热门榜 / 编辑 / 软删除
 *
 * 缓存设计（W5 面试亮点）：
 * 1. 防穿透：查询不存在的帖子时缓存空值（短 TTL），挡掉对不存在 ID 的洪泛查询
 * 2. 防雪崩：过期时间 = 基础值 + 随机值，避免大量 key 同一时刻集体过期打垮 DB
 * 3. 一致性：更新/删除帖子时主动删缓存（Cache-Aside），liked 等用户态数据不进共享缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private static final String POST_CACHE_PREFIX = "post:info:";
    /** 空值缓存占位符 */
    private static final String EMPTY_CACHE_VALUE = "";
    /** 缓存基础过期：5 分钟 */
    private static final long CACHE_BASE_TTL_SECONDS = 300;
    /** 随机附加过期：0~2 分钟（防雪崩） */
    private static final long CACHE_RANDOM_TTL_SECONDS = 120;
    /** 空值缓存过期：1 分钟（防穿透，短一些避免长期挡住新数据） */
    private static final long EMPTY_CACHE_TTL_SECONDS = 60;

    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final LikeService likeService;
    private final HotRankService hotRankService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /** 发布帖子 */
    public Long createPost(CreatePostRequest req) {
        Post post = new Post();
        post.setUserId(UserContext.getUserId());
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        postMapper.insert(post);
        // 新帖子以 0 分入榜，后续互动会刷新热度
        hotRankService.refreshScore(post.getId());
        return post.getId();
    }

    /** 某用户的帖子列表（个人主页用，时间线倒序） */
    public Page<PostVO> pagePostsByUser(Long userId, long page, long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .eq(Post::getUserId, userId)
                .orderByDesc(Post::getCreateTime)
                .orderByDesc(Post::getId);
        Page<Post> result = postMapper.selectPage(new Page<>(page, size), wrapper);
        return convertPage(result);
    }

    /**
     * 时间线分页列表：
     * 按 create_time + id 倒序，正好命中索引 idx_timeline(create_time, id)，
     * 分页排序走索引，避免 filesort（面试可展开讲深分页问题）
     */
    public Page<PostVO> pagePosts(long page, long size) {
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<Post>()
                .orderByDesc(Post::getCreateTime)
                .orderByDesc(Post::getId);
        Page<Post> result = postMapper.selectPage(new Page<>(page, size), wrapper);
        return convertPage(result);
    }

    /** 帖子详情（带缓存：防穿透 + 防雪崩；浏览量始终写库） */
    public PostVO getPostDetail(Long id) {
        String key = POST_CACHE_PREFIX + id;

        // 1. 先读缓存（Redis 故障时降级为直接查库，不影响可用性）
        String cached = null;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("读帖子缓存失败，降级为查库: id={}, err={}", id, e.getMessage());
        }

        PostVO vo = null;
        if (cached != null) {
            if (EMPTY_CACHE_VALUE.equals(cached)) {
                // 空值缓存命中：该 ID 确实不存在，直接拒绝（防穿透）
                throw new BizException(ResultCode.POST_NOT_EXIST);
            }
            try {
                vo = objectMapper.readValue(cached, PostVO.class);
            } catch (Exception e) {
                log.warn("反序列化帖子缓存失败，降级为查库: id={}", id);
            }
        }

        if (vo == null) {
            // 2. 缓存未命中 → 查库 + 回填缓存
            Post post = postMapper.selectById(id);
            if (post == null) {
                cacheEmpty(key);
                throw new BizException(ResultCode.POST_NOT_EXIST);
            }
            vo = toVO(post);
            fillAuthor(List.of(vo));
            cachePost(key, vo);
            // 拿到新鲜数据时顺手刷新热度分
            hotRankService.refreshScore(id);
        }

        // 3. 浏览量：始终原子 +1 写库（缓存里的浏览量是近似值，只作展示）
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, id)
                .setSql("view_count = view_count + 1"));
        vo.setViewCount(vo.getViewCount() + 1);

        // 4. “我是否已点赞”按用户实时计算，绝不进共享缓存
        Long userId = UserContext.getUserId();
        vo.setLiked(userId != null && likeService.hasLiked(id, userId));
        return vo;
    }

    /** 编辑帖子（仅作者） */
    public void updatePost(Long id, UpdatePostRequest req) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_EXIST);
        }
        checkAuthor(post);

        if (req.getTitle() == null && req.getContent() == null) {
            return;
        }
        Post update = new Post();
        update.setId(id);
        update.setTitle(req.getTitle());
        update.setContent(req.getContent());
        // MP 只更新非 null 字段
        postMapper.updateById(update);
        // 主动删缓存：否则标题/内容要等 TTL 过期才生效（Cache-Aside 一致性）
        evictCache(id);
    }

    /** 删除帖子（仅作者；@TableLogic 使其变成软删除） */
    public void deletePost(Long id) {
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_EXIST);
        }
        checkAuthor(post);
        // 执行的其实是 UPDATE post SET deleted=1 WHERE id=?，数据仍在库里
        postMapper.deleteById(id);
        evictCache(id);
        hotRankService.remove(id);
    }

    /** 热门榜 TopN（Redis zset 热度倒序） */
    public List<PostVO> hotPosts(int limit) {
        int n = Math.min(Math.max(limit, 1), 50);
        // 榜单为空（Redis 重启等）时从 DB 惰性重建
        hotRankService.rebuildIfEmpty();
        List<Long> ids = hotRankService.topIds(n);
        if (ids.isEmpty()) {
            return List.of();
        }
        // @TableLogic 自动过滤软删除的帖子
        List<Post> posts = postMapper.selectBatchIds(ids);
        Map<Long, Post> postMap = posts.stream().collect(Collectors.toMap(Post::getId, p -> p));

        // 清理榜单里已不存在的帖子（保持 zset 干净）
        ids.stream().filter(id -> !postMap.containsKey(id)).forEach(hotRankService::remove);

        List<PostVO> vos = ids.stream()
                .map(postMap::get)
                .filter(Objects::nonNull)
                .map(this::toVO)
                .toList();
        fillAuthor(vos);
        return vos;
    }

    /** 缓存帖子信息：基础 TTL + 随机值，避免同一时刻大量 key 集体过期（防雪崩） */
    private void cachePost(String key, PostVO vo) {
        long ttl = CACHE_BASE_TTL_SECONDS
                + ThreadLocalRandom.current().nextLong(CACHE_RANDOM_TTL_SECONDS);
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(vo), ttl, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写帖子缓存失败，跳过: key={}, err={}", key, e.getMessage());
        }
    }

    /** 空值缓存：不存在的帖子也短暂缓存，防穿透 */
    private void cacheEmpty(String key) {
        try {
            redisTemplate.opsForValue().set(key, EMPTY_CACHE_VALUE, EMPTY_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("写空值缓存失败，跳过: key={}, err={}", key, e.getMessage());
        }
    }

    /** 主动删缓存：更新/删除帖子后调用（Redis 故障时仅记录日志，不阻塞主流程） */
    private void evictCache(Long id) {
        try {
            redisTemplate.delete(POST_CACHE_PREFIX + id);
        } catch (Exception e) {
            log.warn("删帖子缓存失败，跳过: id={}, err={}", id, e.getMessage());
        }
    }

    /** 越权校验：不是作者不能改/删 */
    private void checkAuthor(Post post) {
        if (!post.getUserId().equals(UserContext.getUserId())) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
    }

    /** 分页结果转换 + 批量填充作者信息（一次 IN 查询，避免 N+1） */
    private Page<PostVO> convertPage(Page<Post> page) {
        List<PostVO> vos = page.getRecords().stream().map(this::toVO).toList();
        fillAuthor(vos);

        Page<PostVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(vos);
        return result;
    }

    /** 批量填充作者信息：一次 IN 查询取出全部作者，避免逐条查（N+1） */
    private void fillAuthor(List<PostVO> vos) {
        if (vos.isEmpty()) {
            return;
        }
        List<Long> userIds = vos.stream().map(PostVO::getUserId).distinct().toList();
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        vos.forEach(vo -> {
            User author = userMap.get(vo.getUserId());
            if (author != null) {
                vo.setUsername(author.getUsername());
                vo.setNickname(author.getNickname());
                vo.setAvatar(author.getAvatar());
            }
        });
    }

    private PostVO toVO(Post post) {
        PostVO vo = new PostVO();
        BeanUtils.copyProperties(post, vo);
        return vo;
    }
}
