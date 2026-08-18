package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 点赞实体，对应表 post_like
 * 一人一帖最多一条记录：防重靠唯一索引 uk_post_user(post_id, user_id)
 */
@Data
@TableName("post_like")
public class PostLike {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long postId;

    private Long userId;

    private LocalDateTime createTime;
}
