package com.campushub.controller;

import com.campushub.annotation.RequireLogin;
import com.campushub.common.Result;
import com.campushub.dto.UpdateUserRequest;
import com.campushub.service.UserService;
import com.campushub.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：个人信息（需登录，@RequireLogin 控制）
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @RequireLogin
    @GetMapping("/info")
    public Result<UserVO> info() {
        return Result.ok(userService.currentUser());
    }

    @RequireLogin
    @PutMapping("/info")
    public Result<UserVO> updateInfo(@Valid @RequestBody UpdateUserRequest req) {
        return Result.ok(userService.updateInfo(req));
    }
}
