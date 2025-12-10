package com.jibangyoung.domain.mypage.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode // 복합키에서 필수
public class UserRegionScoreId implements Serializable {
    private Long userId;
    private Long regionId;
}