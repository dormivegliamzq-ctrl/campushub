package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体，对应表 `user`
 */
@Data
@TableName("user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    /** 密码密文：序列化 JSON 时永远不输出（安全习惯） */
    @JsonIgnore
    private String password;

    private String nickname;

    private String avatar;

    private String bio;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
