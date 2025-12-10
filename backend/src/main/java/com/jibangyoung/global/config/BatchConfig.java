package com.jibangyoung.global.config;

import java.sql.SQLException;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.jibangyoung.global.batch.UserActivityLogProcessor;
import com.jibangyoung.global.batch.UserActivityLogReader;
import com.jibangyoung.global.batch.UserActivityLogWriter;
import com.jibangyoung.global.common.UserActivityLog;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 🔧 완전 수정된 배치 처리 설정
 */
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
@Slf4j
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final UserActivityLogReader userActivityLogReader;
    private final UserActivityLogProcessor userActivityLogProcessor;
    private final UserActivityLogWriter userActivityLogWriter;

    @PostConstruct
    public void init() {
        log.info("🔧 =================== BatchConfig 초기화 시작 ===================");
        log.info("🔧 Bean 상태 확인:");
        log.info("🔧 - JobRepository: {}", jobRepository != null ? "✅ 정상" : "❌ NULL");
        log.info("🔧 - TransactionManager: {}", transactionManager != null ? "✅ 정상" : "❌ NULL");
        log.info("🔧 - UserActivityLogReader: {}", userActivityLogReader != null ? "✅ 정상" : "❌ NULL");
        log.info("🔧 - UserActivityLogProcessor: {}", userActivityLogProcessor != null ? "✅ 정상" : "❌ NULL");
        log.info("🔧 - UserActivityLogWriter: {}", userActivityLogWriter != null ? "✅ 정상" : "❌ NULL");
        log.info("🔧 =================== BatchConfig 초기화 완료 ===================");
    }

    @Bean
    public Job userActivityLogJob(Step userActivityLogStep) {
        log.info("🔧 userActivityLogJob Bean 생성 중...");
        Job job = new JobBuilder("userActivityLogJob", jobRepository)
                .start(userActivityLogStep)
                .build();
        log.info("🔧 userActivityLogJob Bean 생성 완료: {}", job != null ? "✅ 성공" : "❌ 실패");
        return job;
    }

    @Bean
    public Step userActivityLogStep() {
        log.info("🔧 userActivityLogStep Bean 생성 중...");

        Step step = new StepBuilder("userActivityLogStep", jobRepository)
                .<UserActivityLog, UserActivityLog>chunk(50, transactionManager) // 청크 사이즈 50
                .reader(userActivityLogReader)
                .processor(userActivityLogProcessor)
                .writer(userActivityLogWriter)
                .faultTolerant() // 오류 허용 정책
                .skipLimit(10) // 최대 10개 스킵 허용
                .skip(Exception.class) // 일반 예외는 스킵
                .noSkip(IllegalArgumentException.class) // 필수 데이터 오류는 중단
                .noSkip(SQLException.class) // DB 오류는 중단
                .listener(new StepExecutionListener() { // 스텝 리스너 추가
                    @Override
                    public void beforeStep(StepExecution stepExecution) {
                        log.info("🚀 배치 스텝 시작: {} (JobId: {})",
                                stepExecution.getStepName(),
                                stepExecution.getJobExecutionId());
                    }

                    @Override
                    public ExitStatus afterStep(StepExecution stepExecution) {
                        log.info("🏁 배치 스텝 완료: {} (JobId: {})",
                                stepExecution.getStepName(),
                                stepExecution.getJobExecutionId());
                        log.info("🏁 스텝 결과 상세:");
                        log.info("🏁 - Read Count: {}", stepExecution.getReadCount());
                        log.info("🏁 - Write Count: {}", stepExecution.getWriteCount());
                        log.info("🏁 - Skip Count: {}", stepExecution.getSkipCount());
                        log.info("🏁 - Commit Count: {}", stepExecution.getCommitCount());
                        log.info("🏁 - Rollback Count: {}", stepExecution.getRollbackCount());

                        // 실행 결과 분석
                        if (stepExecution.getSkipCount() > 0) {
                            log.warn("🏁 ⚠️ 스킵된 로그가 있습니다: count={}", stepExecution.getSkipCount());
                        }

                        if (stepExecution.getRollbackCount() > 0) {
                            log.error("🏁 💥 롤백이 발생했습니다: count={}", stepExecution.getRollbackCount());
                        }

                        if (stepExecution.getWriteCount() > 0) {
                            log.info("🏁 ✅ DB 저장 성공: {}개", stepExecution.getWriteCount());
                        } else {
                            log.warn("🏁 ⚠️ DB에 저장된 데이터가 없습니다");
                        }

                        return stepExecution.getExitStatus();
                    }
                })
                .build();

        log.info("🔧 userActivityLogStep Bean 생성 완료: {}", step != null ? "✅ 성공" : "❌ 실패");
        return step;
    }
}