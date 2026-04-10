package com.music163.starter.module.user.vo;

import com.music163.starter.module.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户响应 VO
 * <p>
 * 对外暴露的用户信息，不含 password、deleted 等敏感/内部字段。
 * 使用 {@link #from(User)} 或 {@link #from(User, List)} 从实体转换。
 */
@Data
@Builder
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Integer status;
    private LocalDateTime createdAt;

    /**
     * 用户角色列表（角色 code，如 ["ADMIN", "USER"]）
     */
    @Builder.Default
    private List<String> roles = List.of();

    /**
     * 不含角色信息的转换（用于列表场景）
     */
    public static UserVO from(User user) {
        return from(user, List.of());
    }

    /**
     * 含角色信息的转换（用于当前用户详情场景）
     */
    public static UserVO from(User user, List<String> roles) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .roles(roles)
                .build();
    }
}
