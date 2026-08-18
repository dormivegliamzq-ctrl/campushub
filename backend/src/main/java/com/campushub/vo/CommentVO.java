package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论视图对象：评论内容 + 评论人信息 + 被回复人用户名
 */
@Data
public class CommentVO {

    private Long id;
    private Long postId;
    private Long userId;

    /** 评论人信息 */
    private String username;
    private String nickname;
    private String avatar;

    /** 父评论ID：0=一级评论 */
    private Long parentId;

    /** 被回复的用户ID（仅回复时非空） */
    private Long replyToUserId;

    /** 被回复的用户名（前端显示 "回复 @xxx"） */
    private String replyToUsername;

    private String content;

    private LocalDateTime createTime;
}
