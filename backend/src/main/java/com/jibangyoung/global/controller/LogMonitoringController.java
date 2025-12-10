package com.jibangyoung.global.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jibangyoung.global.common.ApiResponse;
import com.jibangyoung.global.repository.UserActivityLogRedisRepository;
import com.jibangyoung.global.scheduler.LogBatchScheduler;
import com.jibangyoung.global.service.UserActivityLogService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogMonitoringController {

    private final UserActivityLogService logService;
    private final UserActivityLogRedisRepository redisRepository;
    private final LogBatchScheduler scheduler;

    @GetMapping("/queue-status")
    public ApiResponse<Map<String, Object>> getQueueStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("batchQueueSize", logService.getQueueSize());
        status.put("criticalLogCount", redisRepository.getCriticalLogs(100).size());
        status.put("timestamp", System.currentTimeMillis());
        return ApiResponse.success(status);
    }

    @PostMapping("/manual-batch")
    public ApiResponse<String> runManualBatch() {
        try {
            String result = scheduler.runManualBatch();
            return ApiResponse.success(result);
        } catch (Exception e) {
            return ApiResponse.error("배치 실행 실패: " + e.getMessage());
        }
    }
}