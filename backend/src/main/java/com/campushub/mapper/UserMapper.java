package com.campushub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campushub.entity.User;

/**
 * 用户 Mapper：继承 BaseMapper 即拥有全部单表 CRUD，无需写 SQL
 */
public interface UserMapper extends BaseMapper<User> {
}
