package com.campushub.controller;

import com.campushub.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 健康检查接口：W1 验收用
 * 访问 http://localhost:8080/api/health 返回 200 + pong 即代表工程跑通
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<String> health() {
        return Result.ok("pong - " + LocalDateTime.now());
    }
}
