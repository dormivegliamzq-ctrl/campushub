package com.campushub.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑帖子请求体（字段可选，只更新传了的字段）
 */
@Data
public class UpdatePostRequest {

    @Size(max = 64, message = "标题最长64个字符")
    private String title;

    @Size(max = 20000, message = "内容最长20000个字符")
    private String content;
}
