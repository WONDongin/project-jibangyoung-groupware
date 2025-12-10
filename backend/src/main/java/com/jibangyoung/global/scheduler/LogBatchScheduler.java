package com.jibangyoung.global.scheduler;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jibangyoung.global.batch.UserActivityLogReader;
import com.jibangyoung.global.service.UserActivityLogService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 🧪 완전 수정된 배치 처리 스케줄러 (멀티 스레드 지원)
 */
@Component
@Slf4j
public class LogBatchScheduler {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job userActivityLogJob;

    @Autowired
    private UserActivityLogService logService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserActivityLogReader logReader;

    /**
     * Bean 생성 확인 및 초기화
     */
    @PostConstruct
    public void init() {
        log.info("🚀 =================== LogBatchScheduler 초기화 시작 ===================");
        log.info("🚀 현재 스레드: {}", Thread.currentThread().getName());
        log.info("🚀 Bean 상태 확인:");
        log.info("🚀 - JobLauncher: {}", jobLauncher != null ? "✅ 정상" : "❌ NULL");
        log.info("🚀 - userActivityLogJob: {}", userActivityLogJob != null ? "✅ 정상" : "❌ NULL");
        log.info("🚀 - logService: {}", logService != null ? "✅ 정상" : "❌ NULL");
        log.info("🚀 - jdbcTemplate: {}", jdbcTemplate != null ? "✅ 정상" : "❌ NULL");
        log.info("🚀 - logReader: {}", logReader != null ? "✅ 정상" : "❌ NULL");

        // 초기 상태 체크
        checkInitialStatus();

        log.info("🚀 =================== LogBatchScheduler 초기화 완료 ===================");
    }

    /**
     * 🧪 테스트용: 5초마다 실행 (스케줄러 작동 확인용) - 논블로킹
     */
    @Scheduled(fixedRate = 5000)
    public void heartbeat() {
        String threadName = Thread.currentThread().getName();
        log.info("💓 [HEARTBEAT-{}] 스케줄러 정상 작동 중 - {}", threadName, java.time.LocalDateTime.now());

        try {
            if (logService != null) {
                long queueSize = logService.getQueueSize();
                log.info("💓 [HEARTBEAT-{}] Redis 큐 크기: {}", threadName, queueSize);

                if (queueSize > 50) {
                    log.warn("💓 [HEARTBEAT-{}] ⚠️ Redis 큐가 많습니다: {}", threadName, queueSize);
                }
            } else {
                log.error("💓 [HEARTBEAT-{}] logService가 NULL입니다!", threadName);
            }
        } catch (Exception e) {
            log.error("💓 [HEARTBEAT-{}] 오류 발생", threadName, e);
        }
    }

    /**
     * 🎯 메인 배치 실행: 1분마다 실행 - 비동기 처리
     */
    @Scheduled(fixedRate = 60000)
    @Async("schedulerExecutor")
    public void runLogBatch() {
        String threadName = Thread.currentThread().getName();
        long startTime = System.currentTimeMillis();

        log.info("🎯 ================= 배치 실행 시작 [{}] =================", threadName);
        log.info("🎯 실행 시간: {}", java.time.LocalDateTime.now());

        try {
            // 1. 기본 상태 확인
            if (!checkBeansAvailability()) {
                log.error("🎯 [{}] Bean들이 준비되지 않았습니다. 배치를 중단합니다.", threadName);
                return;
            }

            // 2. Redis 큐 크기 확인
            long queueSize = logService.getQueueSize();
            log.info("🎯 [{}] Redis 큐 크기: {}", threadName, queueSize);

            if (queueSize == 0) {
                log.info("🎯 [{}] 처리할 로그가 없어서 배치를 스킵합니다.", threadName);
                return;
            }

            // 3. DB 연결 확인
            checkDatabaseConnection();

            // 4. Reader 리셋
            log.info("🎯 [{}] Reader 상태 리셋 중...", threadName);
            logReader.reset();

            // 5. 배치 Job 실행
            log.info("🎯 [{}] 배치 Job 실행 시작...", threadName);
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addLong("queueSize", queueSize)
                    .addString("trigger", "SCHEDULER_1MIN")
                    .addString("mode", "AUTO_RUN")
                    .addString("thread", threadName)
                    .toJobParameters();

            JobExecution jobExecution = jobLauncher.run(userActivityLogJob, jobParameters);

            // 6. 실행 결과 확인
            long executionTime = System.currentTimeMillis() - startTime;
            long afterQueueSize = logService.getQueueSize();
            long processedCount = queueSize - afterQueueSize;

            log.info("🎯 [{}] 배치 실행 완료!", threadName);
            log.info("🎯 [{}] - 실행 상태: {}", threadName, jobExecution.getStatus());
            log.info("🎯 [{}] - 실행 시간: {}ms", threadName, executionTime);
            log.info("🎯 [{}] - 처리 전 큐: {}", threadName, queueSize);
            log.info("🎯 [{}] - 처리 후 큐: {}", threadName, afterQueueSize);
            log.info("🎯 [{}] - 처리된 로그: {}", threadName, processedCount);

            // 7. 상세 결과 확인
            checkBatchResult(jobExecution, threadName);

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("🎯 [{}] 배치 실행 실패! 실행시간: {}ms", threadName, executionTime, e);
        }

        log.info("🎯 ================= 배치 실행 종료 [{}] =================", threadName);
    }

    /**
     * 📊 상태 모니터링: 30초마다 실행 - 논블로킹
     */
    @Scheduled(fixedRate = 30000)
    public void monitorStatus() {
        String threadName = Thread.currentThread().getName();
        log.info("📊 [모니터-{}] 시스템 상태 체크 중...", threadName);

        try {
            // Redis 상태
            long queueSize = logService.getQueueSize();

            // DB 상태 - 최근 1분간 저장된 로그 수
            String recentLogsSql = """
                    SELECT COUNT(*) FROM user_activity_event
                    WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 MINUTE)
                    """;
            int recentLogs = jdbcTemplate.queryForObject(recentLogsSql, Integer.class);

            // 전체 로그 수
            String totalLogsSql = "SELECT COUNT(*) FROM user_activity_event";
            int totalLogs = jdbcTemplate.queryForObject(totalLogsSql, Integer.class);

            log.info("📊 [모니터-{}] Redis 큐: {}, 최근1분 저장: {}, 전체 로그: {}",
                    threadName, queueSize, recentLogs, totalLogs);

            // 경고 알림
            if (queueSize > 100) {
                log.warn("📊 [모니터-{}] ⚠️ Redis 큐가 많습니다: {}", threadName, queueSize);
            }

            if (recentLogs == 0 && queueSize > 0) {
                log.warn("📊 [모니터-{}] ⚠️ 큐에 데이터는 있지만 DB 저장이 안 되고 있습니다!", threadName);
            }

        } catch (Exception e) {
            log.error("📊 [모니터-{}] 상태 체크 실패", threadName, e);
        }
    }

    /**
     * Bean 가용성 확인
     */
    private boolean checkBeansAvailability() {
        boolean allGood = true;

        if (jobLauncher == null) {
            log.error("❌ JobLauncher가 NULL입니다!");
            allGood = false;
        }

        if (userActivityLogJob == null) {
            log.error("❌ userActivityLogJob이 NULL입니다!");
            allGood = false;
        }

        if (logService == null) {
            log.error("❌ logService가 NULL입니다!");
            allGood = false;
        }

        if (jdbcTemplate == null) {
            log.error("❌ jdbcTemplate이 NULL입니다!");
            allGood = false;
        }

        if (logReader == null) {
            log.error("❌ logReader가 NULL입니다!");
            allGood = false;
        }

        return allGood;
    }

    /**
     * 초기 상태 확인
     */
    private void checkInitialStatus() {
        try {
            // Redis 연결 확인
            if (logService != null) {
                long queueSize = logService.getQueueSize();
                log.info("🚀 초기 Redis 큐 크기: {}", queueSize);
            }

            // DB 연결 확인
            if (jdbcTemplate != null) {
                int dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
                log.info("🚀 DB 연결 상태: ✅ 정상 ({})", dbCheck);

                // 기존 로그 수 확인
                int totalLogs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_activity_event", Integer.class);
                log.info("🚀 기존 로그 총 개수: {}", totalLogs);
            }

        } catch (Exception e) {
            log.error("🚀 초기 상태 확인 실패", e);
        }
    }

    /**
     * DB 연결 상태 확인
     */
    private void checkDatabaseConnection() {
        try {
            int dbCheck = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            log.info("🎯 DB 연결 상태: ✅ 정상 ({})", dbCheck);
        } catch (Exception e) {
            log.error("🎯 DB 연결 실패!", e);
            throw new RuntimeException("DB 연결 실패", e);
        }
    }

    /**
     * 배치 실행 결과 상세 확인
     */
    private void checkBatchResult(JobExecution jobExecution, String threadName) {
        try {
            // Step 실행 결과 수집
            long readCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(step -> step.getReadCount()).sum();
            long writeCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(step -> step.getWriteCount()).sum();
            long skipCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(step -> step.getSkipCount()).sum();
            long commitCount = jobExecution.getStepExecutions().stream()
                    .mapToLong(step -> step.getCommitCount()).sum();

            log.info("🎯 [{}] 배치 상세 결과:", threadName);
            log.info("🎯 [{}] - Read Count: {}", threadName, readCount);
            log.info("🎯 [{}] - Write Count: {}", threadName, writeCount);
            log.info("🎯 [{}] - Skip Count: {}", threadName, skipCount);
            log.info("🎯 [{}] - Commit Count: {}", threadName, commitCount);
            log.info("🎯 [{}] - Exit Code: {}", threadName, jobExecution.getExitStatus().getExitCode());

            // 최근 DB 저장 확인
            String recentSql = "SELECT COUNT(*) FROM user_activity_event WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 SECOND)";
            int recentSaves = jdbcTemplate.queryForObject(recentSql, Integer.class);

            if (recentSaves > 0) {
                log.info("🎯 [{}] ✅ DB 저장 확인: 최근 30초간 {}개 로그 저장됨", threadName, recentSaves);
            } else {
                log.warn("🎯 [{}] ⚠️ DB 저장 확인: 최근 30초간 저장된 로그가 없음", threadName);
            }

        } catch (Exception e) {
            log.error("🎯 [{}] 배치 결과 확인 중 오류", threadName, e);
        }
    }

    /**
     * 🔧 수동 실행용 메서드 (API나 테스트용)
     */
    @Async("schedulerExecutor")
    public String runManualBatch() {
        String threadName = Thread.currentThread().getName();
        log.info("🔧 [{}] 수동 배치 실행 요청됨", threadName);
        try {
            runLogBatch();
            return "🔧 수동 배치 실행 성공 (스레드: " + threadName + ")";
        } catch (Exception e) {
            log.error("🔧 [{}] 수동 배치 실행 실패", threadName, e);
            return "🔧 수동 배치 실행 실패: " + e.getMessage();
        }
    }

    /**
     * 🔧 긴급 배치 실행
     */
    @Async("schedulerExecutor")
    public String runEmergencyBatch() {
        String threadName = Thread.currentThread().getName();
        log.warn("🚨 [{}] 긴급 배치 실행 시작!", threadName);
        try {
            JobParameters emergencyParams = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("type", "EMERGENCY")
                    .addString("trigger", "MANUAL_EMERGENCY")
                    .addString("thread", threadName)
                    .toJobParameters();

            logReader.reset();
            JobExecution jobExecution = jobLauncher.run(userActivityLogJob, emergencyParams);

            log.warn("🚨 [{}] 긴급 배치 실행 완료: 상태={}", threadName, jobExecution.getStatus());
            return "🚨 긴급 배치 실행 완료: " + jobExecution.getStatus() + " (스레드: " + threadName + ")";

        } catch (Exception e) {
            log.error("🚨 [{}] 긴급 배치 실행 실패", threadName, e);
            return "🚨 긴급 배치 실행 실패: " + e.getMessage();
        }
    }
}