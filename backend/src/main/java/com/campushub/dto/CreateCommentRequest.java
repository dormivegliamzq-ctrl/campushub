package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求体
 */
@Data
public class CreateCommentRequest {

    /** 帖子ID（由路径参数在 Controller 中填充；不参与校验——@Valid 在方法体之前执行） */
    private Long postId;

    /** 回复的评论ID；null/0 = 一级评论 */
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 1000, message = "评论最长1000个字符")
    private String content;
}
