package com.jibangyoung.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {

    @NotBlank(message = "사용자명을 입력해주세요.")
    @Size(min = 4, max = 50, message = "사용자명은 4~50자 사이여야 합니다.")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 4, max = 100, message = "비밀번호는 4~100자 사이여야 합니다.")
    private String password;
}

