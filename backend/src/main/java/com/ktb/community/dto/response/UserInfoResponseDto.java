package com.ktb.community.dto.response;

import lombok.Getter;

@Getter
public class UserInfoResponseDto {
    String nickname;
    String email;
    String profileUrl;
    Long userId;

    public UserInfoResponseDto(String nickname, String email,String profileUrl) {
        this.nickname = nickname;
        this.email = email;
        this.profileUrl = profileUrl;
    }
    public UserInfoResponseDto(String nickname, String email,String profileUrl, Long userId) {
        this.nickname = nickname;
        this.email = email;
        this.profileUrl = profileUrl;
        this.userId = userId;
    }
}
