package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.BizException;
import com.campushub.common.ResultCode;
import com.campushub.common.UserContext;
import com.campushub.entity.Follow;
import com.campushub.entity.User;
import com.campushub.mapper.FollowMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 关注业务：关注 / 取关 / 关注列表 / 粉丝列表
 * 防重复关注：预检查 + 唯一索引 uk_follower_followee 兜底（与点赞防重同一套路）
 */
@Service
@RequiredArgsConstructor
public class FollowService {

    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    /** 关注某人 */
    public void follow(Long followeeId) {
        Long me = UserContext.getUserId();
        if (me.equals(followeeId)) {
            throw new BizException(ResultCode.FOLLOW_SELF);
        }
        User target = userMapper.selectById(followeeId);
        if (target == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }

        Long exists = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, me)
                .eq(Follow::getFolloweeId, followeeId));
        if (exists > 0) {
            throw new BizException(ResultCode.ALREADY_FOLLOWED);
        }

        Follow follow = new Follow();
        follow.setFollowerId(me);
        follow.setFolloweeId(followeeId);
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 并发重复关注：唯一索引兜底
            throw new BizException(ResultCode.ALREADY_FOLLOWED);
        }
    }

    /** 取关某人 */
    public void unfollow(Long followeeId) {
        Long me = UserContext.getUserId();
        int rows = followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, me)
                .eq(Follow::getFolloweeId, followeeId));
        if (rows == 0) {
            throw new BizException(ResultCode.NOT_FOLLOWED);
        }
    }

    /** 我关注的人 */
    public List<UserVO> following() {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, UserContext.getUserId())
                .orderByDesc(Follow::getCreateTime));
        return toUserVOList(follows.stream().map(Follow::getFolloweeId).toList());
    }

    /** 关注我的人（粉丝） */
    public List<UserVO> followers() {
        List<Follow> follows = followMapper.selectList(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFolloweeId, UserContext.getUserId())
                .orderByDesc(Follow::getCreateTime));
        return toUserVOList(follows.stream().map(Follow::getFollowerId).toList());
    }

    /** 按关注关系顺序批量组装用户 VO（保持关注时间倒序） */
    private List<UserVO> toUserVOList(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));
        return userIds.stream()
                .map(userMap::get)
                .filter(Objects::nonNull)
                .map(u -> {
                    UserVO vo = new UserVO();
                    BeanUtils.copyProperties(u, vo);
                    return vo;
                })
                .toList();
    }
}
