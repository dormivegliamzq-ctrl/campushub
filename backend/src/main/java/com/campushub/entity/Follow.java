package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 关注实体，对应表 follow
 * follower（粉丝）关注 followee（被关注者）
 * 防重复关注：唯一索引 uk_follower_followee(follower_id, followee_id)
 */
@Data
@TableName("follow")
public class Follow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 粉丝（发起关注的人） */
    private Long followerId;

    /** 被关注的人 */
    private Long followeeId;

    private LocalDateTime createTime;
}
