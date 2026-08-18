package com.campushub.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象：返回给前端的用户数据
 * （与实体 User 的区别：不含密码字段，防止敏感信息泄露）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private LocalDateTime createTime;
}
