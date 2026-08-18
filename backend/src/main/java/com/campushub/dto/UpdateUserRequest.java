package com.campushub.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改个人信息请求体（字段均可选，只更新传了的字段）
 */
@Data
public class UpdateUserRequest {

    @Size(max = 32, message = "昵称最长32个字符")
    private String nickname;

    @Size(max = 255, message = "头像URL过长")
    private String avatar;

    @Size(max = 255, message = "个性签名最长255个字符")
    private String bio;
}
