package com.jibangyoung.global.common;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 📊 사용자 활동 로그 엔티티 (수정: userId null 허용, 유효성 검증 완화)
 */
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityLog {

    private Long id;
    private Long userId; // null 허용 (익명 사용자)
    private Long regionId; // Writer에서 안전 변환

    private String actionType;
    private Long refId;
    private Long parentRefId;
    private Object actionValue;
    private Integer scoreDelta;
    private String meta;
    private String ipAddr;
    private String userAgent;
    private String platform;
    private String lang;
    private String status;
    private String memo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime updatedAt;

    // tracing fields
    private String logId;
    private String priority;
    private String methodName;
    private String className;
    private Long executionTime;
    private String errorMessage;
    private Map<String, Object> requestParams;
    private Long ttl;

    /** 성공 로그 — 기본값/시간 보강, userId null 허용 */
    public static UserActivityLog success(UserActivityLogBuilder builder) {
        LocalDateTime now = LocalDateTime.now();
        UserActivityLog log = builder
                .status("SUCCESS")
                .createdAt(now)
                .updatedAt(now)
                .build();
        return withDefaults(log);
    }

    /** 실패 로그 — 기본값/시간 보강, userId null 허용 */
    public static UserActivityLog failure(UserActivityLogBuilder builder, String errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        UserActivityLog log = builder
                .status("FAILURE")
                .errorMessage(errorMessage)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return withDefaults(log);
    }

    private static UserActivityLog withDefaults(UserActivityLog src) {
        return src.toBuilder()
                .actionType((src.actionType == null || src.actionType.trim().isEmpty()) ? "UNKNOWN" : src.actionType)
                .scoreDelta(src.scoreDelta == null ? 0 : src.scoreDelta)
                // userId는 null 허용 (익명 사용자)
                .regionId(src.regionId == null ? 0L : src.regionId)
                .build();
    }

    /** 수정된 유효성 검증 - userId null 허용 */
    public boolean isValid() {
        // logId, actionType, createdAt만 필수
        // userId는 null 허용 (익명 사용자)
        return logId != null &&
                actionType != null &&
                !actionType.trim().isEmpty() &&
                createdAt != null;
    }

    public Integer getRegionIdAsInteger() {
        if (regionId == null)
            return 0;
        if (regionId > Integer.MAX_VALUE || regionId < Integer.MIN_VALUE)
            return 0;
        return regionId.intValue();
    }

    public Integer getActionValueAsInteger() {
        if (actionValue == null)
            return null;
        try {
            if (actionValue instanceof Number)
                return ((Number) actionValue).intValue();
            String s = actionValue.toString().trim();
            return s.isEmpty() ? null : Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** DB 저장을 위한 userId 반환 - null이면 0으로 변환 */
    public Long getUserIdForDb() {
        return userId != null ? userId : 0L;
    }
}