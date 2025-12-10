package com.jibangyoung.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

import lombok.extern.slf4j.Slf4j;

/**
 * 🚀 스케줄링 설정 (완전 수정: 멀티 스레드 지원)
 */
@Configuration
@EnableScheduling
@Slf4j
public class SchedulingConfig implements SchedulingConfigurer {

    /**
     * 메인 스케줄러 - 멀티 스레드 지원
     */
    @Bean("schedulerTaskScheduler")
    public TaskScheduler schedulerTaskScheduler() {
        log.info("🚀 스케줄러 TaskScheduler 초기화 시작");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 충분한 스레드 풀 크기
        scheduler.setThreadNamePrefix("scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            log.warn("🚀 스케줄러 작업 거부됨: {}", r.toString());
        });

        scheduler.initialize();

        log.info("🚀 스케줄러 TaskScheduler 초기화 완료: poolSize=10, threadPrefix=scheduler-");
        return scheduler;
    }

    /**
     * 배치 전용 스케줄러 - 별도 스레드 풀
     */
    @Bean("batchTaskScheduler")
    public TaskScheduler batchTaskScheduler() {
        log.info("🚀 배치 TaskScheduler 초기화 시작");

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5); // 배치 전용
        scheduler.setThreadNamePrefix("batch-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60); // 배치는 더 긴 대기 시간
        scheduler.setRejectedExecutionHandler((r, executor) -> {
            log.warn("🚀 배치 스케줄러 작업 거부됨: {}", r.toString());
        });

        scheduler.initialize();

        log.info("🚀 배치 TaskScheduler 초기화 완료: poolSize=5, threadPrefix=batch-scheduler-");
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        // 메인 스케줄러를 기본으로 설정
        registrar.setTaskScheduler(schedulerTaskScheduler());
        log.info("🚀 ScheduledTaskRegistrar 설정 완료");
    }

    /**
     * 추가 Executor for @Async 작업 - ThreadPoolTaskExecutor 사용
     */
    @Bean("schedulerExecutor")
    public Executor schedulerExecutor() {
        log.info("🚀 스케줄러 Executor 초기화");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("scheduler-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        return executor;
    }
}