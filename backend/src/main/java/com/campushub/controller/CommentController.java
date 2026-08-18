package com.campushub.controller;

import com.campushub.annotation.RequireLogin;
import com.campushub.common.Result;
import com.campushub.dto.CreateCommentRequest;
import com.campushub.service.CommentService;
import com.campushub.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 评论接口：
 * - 查看评论列表：游客可访问
 * - 发表/删除评论：需登录（删除仅限本人）
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @RequireLogin
    @PostMapping("/posts/{postId}/comments")
    public Result<Long> create(@PathVariable Long postId, @Valid @RequestBody CreateCommentRequest req) {
        req.setPostId(postId);
        return Result.ok(commentService.createComment(req));
    }

    @GetMapping("/posts/{postId}/comments")
    public Result<List<CommentVO>> list(@PathVariable Long postId) {
        return Result.ok(commentService.listComments(postId));
    }

    @RequireLogin
    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.ok();
    }
}
