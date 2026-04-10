package com.music163.starter.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.music163.starter.module.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户
     */
    @Select("SELECT * FROM users WHERE username = #{username} AND deleted = 0")
    User selectByUsername(String username);
}
