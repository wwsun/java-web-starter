package com.music163.starter.module.role.vo;

import com.music163.starter.module.role.entity.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RoleVO {

    private Long id;
    private String code;

    public static RoleVO from(Role role) {
        return RoleVO.builder()
                .id(role.getId())
                .code(role.getCode())
                .build();
    }
}
