package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.campushub.common.BizException;
import com.campushub.common.ResultCode;
import com.campushub.common.UserContext;
import com.campushub.dto.CreateCommentRequest;
import com.campushub.entity.Comment;
import com.campushub.entity.Post;
import com.campushub.entity.User;
import com.campushub.mapper.CommentMapper;
import com.campushub.mapper.PostMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论业务：发表（两级回复）/ 列表 / 删除
 * 关键点：评论写入 + 帖子评论数更新在同一个事务里，保证计数一致
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserMapper userMapper;
    private final HotRankService hotRankService;

    /** 发表评论（支持回复：parentId 指向被回复的评论） */
    @Transactional
    public Long createComment(CreateCommentRequest req) {
        Post post = postMapper.selectById(req.getPostId());
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_EXIST);
        }

        Comment comment = new Comment();
        comment.setPostId(req.getPostId());
        comment.setUserId(UserContext.getUserId());
        comment.setParentId(req.getParentId() == null ? 0L : req.getParentId());
        comment.setContent(req.getContent());

        // 回复时：被回复人由父评论推导（服务端推导，不信任客户端传值）
        if (comment.getParentId() != 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent == null || !parent.getPostId().equals(req.getPostId())) {
                throw new BizException(ResultCode.COMMENT_NOT_EXIST);
            }
            comment.setReplyToUserId(parent.getUserId());
        }

        commentMapper.insert(comment);

        // 同一事务内更新帖子评论数：要么都成功，要么都回滚
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, req.getPostId())
                .setSql("comment_count = comment_count + 1"));
        // 评论影响热度，刷新榜单
        hotRankService.refreshScore(req.getPostId());
        return comment.getId();
    }

    /** 某帖子的评论列表（按时间正序，最多 200 条，前端自行组装两级结构） */
    public List<CommentVO> listComments(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null) {
            throw new BizException(ResultCode.POST_NOT_EXIST);
        }

        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getPostId, postId)
                .orderByAsc(Comment::getCreateTime)
                .last("LIMIT 200"));

        // 批量取评论人和被回复人（一次 IN 查询，避免 N+1）
        Set<Long> userIds = new HashSet<>();
        comments.forEach(c -> {
            userIds.add(c.getUserId());
            if (c.getReplyToUserId() != null) {
                userIds.add(c.getReplyToUserId());
            }
        });
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return comments.stream().map(c -> {
            CommentVO vo = new CommentVO();
            BeanUtils.copyProperties(c, vo);
            User commenter = userMap.get(c.getUserId());
            if (commenter != null) {
                vo.setUsername(commenter.getUsername());
                vo.setNickname(commenter.getNickname());
                vo.setAvatar(commenter.getAvatar());
            }
            if (c.getReplyToUserId() != null) {
                User replied = userMap.get(c.getReplyToUserId());
                if (replied != null) {
                    vo.setReplyToUsername(replied.getUsername());
                }
            }
            return vo;
        }).toList();
    }

    /** 删除评论（仅评论人；软删除 + 评论数回减） */
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null) {
            throw new BizException(ResultCode.COMMENT_NOT_EXIST);
        }
        if (!comment.getUserId().equals(UserContext.getUserId())) {
            throw new BizException(ResultCode.NO_PERMISSION);
        }
        commentMapper.deleteById(id);

        // 计数回减，带防负保护（IF 函数），保证计数不为负数
        postMapper.update(null, new LambdaUpdateWrapper<Post>()
                .eq(Post::getId, comment.getPostId())
                .setSql("comment_count = IF(comment_count > 0, comment_count - 1, 0)"));
        hotRankService.refreshScore(comment.getPostId());
    }
}
