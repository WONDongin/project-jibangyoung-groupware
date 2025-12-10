package com.jibangyoung.domain.auth.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateRequestDto {

    @Size(min = 2, max = 50, message = "닉네임은 2~50자 사이여야 합니다.")
    private String nickname;

    @Pattern(regexp = "^[0-9]{10,11}$", message = "올바른 전화번호 형식을 입력해주세요.")
    private String phone;

    private String profileImageUrl;
}
