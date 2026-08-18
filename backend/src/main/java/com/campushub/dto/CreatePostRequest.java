package com.campushub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发帖请求体
 */
@Data
public class CreatePostRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 64, message = "标题最长64个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 20000, message = "内容最长20000个字符")
    private String content;
}
