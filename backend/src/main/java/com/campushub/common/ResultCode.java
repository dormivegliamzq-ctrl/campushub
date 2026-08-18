package com.campushub.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务状态码枚举：新增错误场景在这里登记，禁止在业务代码里写魔法数字
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "success"),

    // ---------- 通用 4xx ----------
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限"),
    NOT_FOUND(404, "资源不存在"),

    // ---------- 用户模块 ----------
    USER_NOT_EXIST(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "用户名或密码错误"),
    USERNAME_EXISTS(1003, "用户名已存在"),

    // ---------- 帖子模块 ----------
    POST_NOT_EXIST(1101, "帖子不存在"),
    NO_PERMISSION(1102, "无权进行此操作"),

    // ---------- 评论模块 ----------
    COMMENT_NOT_EXIST(1201, "评论不存在"),

    // ---------- 点赞模块 ----------
    ALREADY_LIKED(1301, "已点赞，请勿重复操作"),
    NOT_LIKED(1302, "尚未点赞"),

    // ---------- 关注模块 ----------
    ALREADY_FOLLOWED(1401, "已关注该用户"),
    NOT_FOLLOWED(1402, "尚未关注该用户"),
    FOLLOW_SELF(1403, "不能关注自己"),

    // ---------- 5xx ----------
    SYSTEM_ERROR(500, "系统繁忙，请稍后重试");

    private final Integer code;
    private final String message;
}
