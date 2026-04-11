package com.music163.starter.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新用户信息请求
 * <p>
 * 所有字段均为可选，传入非空值时才更新对应字段。
 */
@Data
public class UpdateUserRequest {

    @Size(max = 64, message = "昵称长度不能超过 64 个字符")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String avatar;
}
