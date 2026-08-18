package com.campushub.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campushub.common.Result;
import com.campushub.service.PostService;
import com.campushub.service.UserService;
import com.campushub.vo.PostVO;
import com.campushub.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户主页接口（游客可看；登录用户会额外返回"我是否已关注TA"）
 */
@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final PostService postService;

    @GetMapping("/api/users/{id}")
    public Result<UserProfileVO> profile(@PathVariable Long id) {
        return Result.ok(userService.getUserProfile(id));
    }

    /** 某用户的帖子列表（个人主页用，游客可看） */
    @GetMapping("/api/users/{id}/posts")
    public Result<Page<PostVO>> userPosts(@PathVariable Long id,
                                          @RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(postService.pagePostsByUser(id, page, size));
    }
}
