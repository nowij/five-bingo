package com.fivebingo.dto;

import lombok.Data;

public class AuthDto {

    @Data
    public static class SignupRequest {
        private String username;
        private String password;
        private String nickname;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class AuthResponse {
        private String token;
        private String nickname;
        private Long userId;

        public AuthResponse(String token, String nickname, Long userId) {
            this.token = token;
            this.nickname = nickname;
            this.userId = userId;
        }
    }
}

