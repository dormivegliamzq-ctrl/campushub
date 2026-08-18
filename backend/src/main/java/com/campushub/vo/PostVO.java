package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子视图对象：帖子数据 + 作者信息（用户名/昵称/头像）
 */
@Data
public class PostVO {

    private Long id;
    private Long userId;

    /** 作者信息（由 Service 批量查询填充，避免 N+1） */
    private String username;
    private String nickname;
    private String avatar;

    private String title;
    private String content;

    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;

    /** 当前登录用户是否已点赞（游客为 false；详情接口填充） */
    private Boolean liked;

    private LocalDateTime createTime;
}
