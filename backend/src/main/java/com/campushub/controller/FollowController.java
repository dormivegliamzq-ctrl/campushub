package com.campushub.controller;

import com.campushub.annotation.RequireLogin;
import com.campushub.common.Result;
import com.campushub.service.FollowService;
import com.campushub.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 关注接口（均需登录）
 */
@RestController
@RequestMapping("/api/follow")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @RequireLogin
    @PostMapping("/{userId}")
    public Result<Void> follow(@PathVariable Long userId) {
        followService.follow(userId);
        return Result.ok();
    }

    @RequireLogin
    @DeleteMapping("/{userId}")
    public Result<Void> unfollow(@PathVariable Long userId) {
        followService.unfollow(userId);
        return Result.ok();
    }

    /** 我关注的人 */
    @RequireLogin
    @GetMapping("/following")
    public Result<List<UserVO>> following() {
        return Result.ok(followService.following());
    }

    /** 关注我的人（粉丝） */
    @RequireLogin
    @GetMapping("/followers")
    public Result<List<UserVO>> followers() {
        return Result.ok(followService.followers());
    }
}
