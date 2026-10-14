package com.instakill.api.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CommentForm {
    @NotBlank
    @Size(max = 500)
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
