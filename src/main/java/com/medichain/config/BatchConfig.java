package com.medichain.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class BatchConfig {

    private final JobLauncher jobLauncher;
    private final Job expiryAuditJob;
    private final Job aiForecastBatchJob;

    @Scheduled(cron = "${medichain.batch.expiry-audit.cron}")
    public void runExpiryAuditJob() {
        try {
            var params = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(expiryAuditJob, params);
        } catch (Exception e) {
            throw new RuntimeException("Expiry audit job failed", e);
        }
    }

    @Scheduled(cron = "${medichain.batch.forecast.cron}")
    public void runAiForecastJob() {
        try {
            var params = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();
            jobLauncher.run(aiForecastBatchJob, params);
        } catch (Exception e) {
            throw new RuntimeException("AI forecast batch job failed", e);
        }
    }
}
