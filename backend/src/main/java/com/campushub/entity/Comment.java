package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论实体，对应表 comment
 * 两级结构：parent_id=0 是一级评论；否则是对某条评论的回复
 */
@Data
@TableName("comment")
public class Comment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属帖子ID */
    private Long postId;

    /** 评论人用户ID */
    private Long userId;

    /** 父评论ID：0=一级评论，>0=回复的评论ID */
    private Long parentId;

    /** 被回复的用户ID（由服务端根据父评论推导，不信任客户端传值） */
    private Long replyToUserId;

    private String content;

    /** 软删除标记 */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;
}
