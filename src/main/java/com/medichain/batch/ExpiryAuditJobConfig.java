package com.medichain.batch;

import com.medichain.domain.entity.DrugBatch;
import com.medichain.domain.entity.Enums.AlertSeverity;
import com.medichain.domain.entity.ExpiryAlertRecord;
import com.medichain.domain.repository.DrugBatchRepository;
import com.medichain.domain.repository.ExpiryAlertRecordRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class ExpiryAuditJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final ExpiryAlertRecordRepository alertRecordRepository;

    @Value("${medichain.batch.expiry-audit.chunk-size:100}")
    private int chunkSize;

    @Value("${medichain.inventory.critical-expiry-window-days:60}")
    private int expiryWindowDays;

    @Bean
    public Job expiryAuditJob() {
        return new JobBuilder("expiryAuditJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(expiryAuditStep())
            .build();
    }

    @Bean
    public Step expiryAuditStep() {
        return new StepBuilder("expiryAuditStep", jobRepository)
            .<DrugBatch, ExpiryAlertRecord>chunk(chunkSize, transactionManager)
            .reader(expiryBatchReader())
            .processor(expiryClassifierProcessor())
            .writer(expiryAlertWriter())
            .faultTolerant()
            .skip(Exception.class, 5)
            .retry(Exception.class, 3)
            .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<DrugBatch> expiryBatchReader() {
        var cutoffDate = LocalDate.now().plusDays(expiryWindowDays);
        var params = Map.<String, Object>of("cutoffDate", cutoffDate);

        return new JpaPagingItemReaderBuilder<DrugBatch>()
            .name("expiryBatchReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT b FROM DrugBatch b JOIN FETCH b.drugSku JOIN FETCH b.shelf s JOIN FETCH s.ward " +
                          "WHERE b.expiryDate < :cutoffDate AND b.quantityOnHand > 0 ORDER BY b.expiryDate ASC")
            .parameterValues(params)
            .pageSize(chunkSize)
            .saveState(true)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<DrugBatch, ExpiryAlertRecord> expiryClassifierProcessor() {
        return batch -> {
            var now = LocalDate.now();
            var daysToExpiry = (int) java.time.temporal.ChronoUnit.DAYS.between(now, batch.getExpiryDate());

            if (daysToExpiry < 0 || daysToExpiry > expiryWindowDays) {
                return null;
            }

            var severity = daysToExpiry < 30 ? AlertSeverity.CRITICAL
                : daysToExpiry < 45 ? AlertSeverity.WARNING
                : AlertSeverity.WATCH;

            var record = new ExpiryAlertRecord();
            record.setDrugBatch(batch);
            record.setSeverity(severity);
            record.setDaysToExpiry(daysToExpiry);
            record.setAlertMessage(String.format(
                "Batch %s of %s (Shelf: %s, Ward: %s) expires in %d days on %s. %d units remaining.",
                batch.getBatchNumber(),
                batch.getDrugSku().getGenericName(),
                batch.getShelf().getShelfCode(),
                batch.getShelf().getWard().getName(),
                daysToExpiry, batch.getExpiryDate(),
                batch.getQuantityOnHand()));
            record.setResolved(false);

            return record;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<ExpiryAlertRecord> expiryAlertWriter() {
        return chunk -> {
            var items = chunk.getItems();
            alertRecordRepository.saveAll(items);
            log.info("ExpiryAuditJob: processed {} expiry alerts", items.size());
        };
    }
}
