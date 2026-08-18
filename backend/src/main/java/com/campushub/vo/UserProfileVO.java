package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户主页视图对象：用户信息 + 发帖/关注/粉丝统计 + 当前用户是否已关注 TA
 */
@Data
public class UserProfileVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private LocalDateTime createTime;

    /** 发帖数 */
    private Long postCount;

    /** 关注数 */
    private Long followingCount;

    /** 粉丝数 */
    private Long followerCount;

    /** 当前登录用户是否已关注 TA（游客恒为 false） */
    private Boolean followed;
}
