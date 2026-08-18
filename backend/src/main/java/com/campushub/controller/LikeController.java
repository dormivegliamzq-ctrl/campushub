package com.campushub.controller;

import com.campushub.annotation.RequireLogin;
import com.campushub.common.Result;
import com.campushub.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 点赞接口（需登录）
 * - POST   /api/posts/{postId}/like    点赞
 * - DELETE /api/posts/{postId}/like    取消点赞
 */
@RestController
@RequestMapping("/api/posts/{postId}/like")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @RequireLogin
    @PostMapping
    public Result<Void> like(@PathVariable Long postId) {
        likeService.like(postId);
        return Result.ok();
    }

    @RequireLogin
    @DeleteMapping
    public Result<Void> unlike(@PathVariable Long postId) {
        likeService.unlike(postId);
        return Result.ok();
    }
}
