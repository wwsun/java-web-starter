package com.music163.starter.module.user.vo;

import com.music163.starter.module.user.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户响应 VO
 * <p>
 * 对外暴露的用户信息，不含 password、deleted 等敏感/内部字段。
 * 使用 {@link #from(User)} 从实体转换，避免在 Controller/Service 中手动 set 字段。
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
     * 从 User 实体转换为 VO（标准转换入口）
     */
    public static UserVO from(User user) {
        return UserVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
