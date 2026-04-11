package com.music163.starter.role;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.music163.starter.role.Role;
import com.music163.starter.role.RoleMapper;
import com.music163.starter.role.RoleService;
import com.music163.starter.role.RoleVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        return baseMapper.selectRoleCodesByUserId(userId);
    }

    @Override
    public List<RoleVO> listAll() {
        return list().stream().map(RoleVO::from).toList();
    }

    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        baseMapper.deleteUserRoles(userId);
        for (Long roleId : roleIds) {
            baseMapper.insertUserRole(userId, roleId);
        }
    }

    @Override
    public void assignDefaultRole(Long userId) {
        Role userRole = lambdaQuery().eq(Role::getCode, "USER").one();
        if (userRole != null) {
            baseMapper.insertUserRole(userId, userRole.getId());
        }
    }
}
