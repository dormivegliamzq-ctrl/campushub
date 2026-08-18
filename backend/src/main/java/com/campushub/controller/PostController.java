package com.campushub.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.annotation.RequireLogin;
import com.campushub.common.Result;
import com.campushub.dto.CreatePostRequest;
import com.campushub.dto.UpdatePostRequest;
import com.campushub.service.PostService;
import com.campushub.vo.PostVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
/**
 * 帖子接口：
 * - 列表 / 详情：游客可访问（论坛需要能被浏览）
 * - 发布 / 编辑 / 删除：必须登录（@RequireLogin）
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @RequireLogin
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CreatePostRequest req) {
        return Result.ok(postService.createPost(req));
    }

    /** 热门榜 TopN（zset 热度倒序） */
    @GetMapping("/hot")
    public Result<List<PostVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(postService.hotPosts(limit));
    }

    @GetMapping
    public Result<Page<PostVO>> page(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size) {
        return Result.ok(postService.pagePosts(page, size));
    }

    @GetMapping("/{id}")
    public Result<PostVO> detail(@PathVariable Long id) {
        return Result.ok(postService.getPostDetail(id));
    }

    @RequireLogin
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdatePostRequest req) {
        postService.updatePost(id, req);
        return Result.ok();
    }

    @RequireLogin
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.ok();
    }
}
