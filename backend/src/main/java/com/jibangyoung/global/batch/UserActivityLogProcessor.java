package com.jibangyoung.global.batch;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import com.jibangyoung.global.common.UserActivityLog;

import lombok.extern.slf4j.Slf4j;

/**
 * 로그 데이터 전처리 (userId null 허용)
 */
@Component
@Slf4j
public class UserActivityLogProcessor implements ItemProcessor<UserActivityLog, UserActivityLog> {

    @Override
    public UserActivityLog process(UserActivityLog item) throws Exception {

        if (!isValidLog(item)) {
            log.warn("유효하지 않은 로그 스킵: logId={}, actionType={}, createdAt={}",
                    item.getLogId(), item.getActionType(), item.getCreatedAt());
            return null;
        }

        log.debug("로그 처리 중: logId={}, actionType={}, userId={}",
                item.getLogId(), item.getActionType(), item.getUserId());

        return UserActivityLog.builder()
                .logId(item.getLogId())
                .userId(item.getUserId()) // null 허용
                .regionId(item.getRegionId())
                .actionType(item.getActionType())
                .refId(item.getRefId())
                .parentRefId(item.getParentRefId())
                .actionValue(item.getActionValue())
                .scoreDelta(item.getScoreDelta())
                .meta(sanitizeMeta(item.getMeta()))
                .ipAddr(item.getIpAddr())
                .userAgent(truncateUserAgent(item.getUserAgent()))
                .platform(item.getPlatform())
                .lang(item.getLang())
                .status(item.getStatus())
                .memo(item.getMemo())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    /**
     * 수정된 유효성 검증 - userId null 허용
     */
    private boolean isValidLog(UserActivityLog entry) {
        // logId, actionType, createdAt만 필수 체크 (userId는 null 허용)
        boolean valid = entry.getLogId() != null &&
                entry.getActionType() != null &&
                !entry.getActionType().trim().isEmpty() &&
                entry.getCreatedAt() != null;

        if (!valid) {
            // 여기의 log는 Lombok @Slf4j 로거를 가리킴
            log.warn("유효성 검증 실패: logId={}, actionType={}, createdAt={}",
                    entry.getLogId(), entry.getActionType(), entry.getCreatedAt());
        }

        return valid;
    }

    private String sanitizeMeta(String meta) {
        if (meta != null && meta.length() > 2000) {
            log.debug("meta 필드 길이 조정: 원본={}, 조정후=2000", meta.length());
            return meta.substring(0, 2000) + "...";
        }
        return meta;
    }

    private String truncateUserAgent(String userAgent) {
        if (userAgent != null && userAgent.length() > 500) {
            log.debug("userAgent 필드 길이 조정: 원본={}, 조정후=500", userAgent.length());
            return userAgent.substring(0, 500);
        }
        return userAgent;
    }
}