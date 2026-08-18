package com.campushub.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campushub.common.BizException;
import com.campushub.common.ResultCode;
import com.campushub.common.UserContext;
import com.campushub.dto.LoginRequest;
import com.campushub.dto.RegisterRequest;
import com.campushub.dto.UpdateUserRequest;
import com.campushub.entity.Follow;
import com.campushub.entity.Post;
import com.campushub.entity.User;
import com.campushub.mapper.FollowMapper;
import com.campushub.mapper.PostMapper;
import com.campushub.mapper.UserMapper;
import com.campushub.util.JwtUtil;
import com.campushub.vo.LoginVO;
import com.campushub.vo.UserProfileVO;
import com.campushub.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户业务：注册 / 登录 / 个人信息
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PostMapper postMapper;
    private final FollowMapper followMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /** 注册：密码 BCrypt 加密后入库 */
    public void register(RegisterRequest req) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        User user = new User();
        user.setUsername(req.getUsername());
        // 数据库里永远只存密文，不存明文
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同一用户名：预检查挡不住竞态，唯一索引是最后防线
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }
    }

    /** 登录：校验密码 → 签发 JWT */
    public LoginVO login(LoginRequest req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, req.getUsername()));

        // 用户不存在和密码错误统一提示，不暴露"到底是哪个错了"（安全最佳实践）
        if (user == null || !passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PASSWORD_ERROR, "用户名或密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new LoginVO(token, toVO(user));
    }

    /** 查询当前登录用户信息（用户身份来自拦截器写入的 UserContext） */
    public UserVO currentUser() {
        User user = userMapper.selectById(UserContext.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        return toVO(user);
    }

    /** 修改个人信息：MyBatis-Plus 只更新非 null 字段 */
    public UserVO updateInfo(UpdateUserRequest req) {
        // 三个字段全没传 → 无需更新
        if (req.getNickname() == null && req.getAvatar() == null && req.getBio() == null) {
            return currentUser();
        }
        User user = new User();
        user.setId(UserContext.getUserId());
        user.setNickname(req.getNickname());
        user.setAvatar(req.getAvatar());
        user.setBio(req.getBio());
        userMapper.updateById(user);
        return currentUser();
    }

    /** 用户主页：基本信息 + 统计 + 当前登录用户是否已关注 TA */
    public UserProfileVO getUserProfile(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_EXIST);
        }
        UserProfileVO vo = new UserProfileVO();
        BeanUtils.copyProperties(user, vo);

        vo.setPostCount(postMapper.selectCount(
                new LambdaQueryWrapper<Post>().eq(Post::getUserId, id)));
        vo.setFollowingCount(followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, id)));
        vo.setFollowerCount(followMapper.selectCount(
                new LambdaQueryWrapper<Follow>().eq(Follow::getFolloweeId, id)));

        Long me = UserContext.getUserId();
        vo.setFollowed(me != null && !me.equals(id)
                && followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, me)
                        .eq(Follow::getFolloweeId, id)) > 0);
        return vo;
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
