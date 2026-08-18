package com.campushub.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 帖子实体，对应表 post
 */
@Data
@TableName("post")
public class Post {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作者用户ID */
    private Long userId;

    private String title;

    private String content;

    /** 浏览量（详情接口原子 +1） */
    private Integer viewCount;

    /** 点赞数（W4 事务维护） */
    private Integer likeCount;

    /** 评论数（W4 事务维护） */
    private Integer commentCount;

    /**
     * 软删除标记：1=已删除。
     * @TableLogic 会让 MP 的所有查询自动附加 deleted=0，
     * 且 deleteById 自动变成 UPDATE deleted=1（而不是真删）
     */
    @TableLogic
    private Integer deleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
