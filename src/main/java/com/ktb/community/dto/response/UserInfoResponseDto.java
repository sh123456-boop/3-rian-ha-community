package com.ktb.community.dto.response;

import lombok.Getter;

@Getter
public class UserInfoResponseDto {
    String nickname;
    String profileUrl;

    public UserInfoResponseDto(String nickname, String profileUrl) {
        this.nickname = nickname;
        this.profileUrl = profileUrl;
    }
}
