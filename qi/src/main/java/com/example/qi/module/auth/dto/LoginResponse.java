package com.example.qi.module.auth.dto;

import lombok.Data;

@Data
public class LoginResponse {

    /** Sa-Token 令牌，前端后续请求放在 Authorization 头 */
    private String token;

    private String displayName;
}
