package com.medichain.batch;

import com.medichain.ai.AiForecastService;
import com.medichain.ai.ForecastPersistenceService;
import com.medichain.domain.entity.AiForecastReport;
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
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AiForecastBatchJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final ForecastPersistenceService forecastPersistenceService;
    private final AiForecastService forecastService;

    @Value("${medichain.batch.forecast.chunk-size:50}")
    private int chunkSize;

    @Bean
    public Job aiForecastBatchJob() {
        return new JobBuilder("aiForecastBatchJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .start(aiForecastStep())
            .build();
    }

    @Bean
    public Step aiForecastStep() {
        return new StepBuilder("aiForecastStep", jobRepository)
            .<WardSkuPair, AiForecastReport>chunk(chunkSize, transactionManager)
            .reader(forecastCandidateReader())
            .processor(forecastProcessor())
            .writer(forecastWriter())
            .faultTolerant()
            .skip(Exception.class, 10)
            .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<WardSkuPair> forecastCandidateReader() {
        return new JpaPagingItemReaderBuilder<WardSkuPair>()
            .name("forecastCandidateReader")
            .entityManagerFactory(entityManagerFactory)
            .queryString("SELECT DISTINCT NEW com.medichain.batch.WardSkuPair(" +
                         "s.ward.id, b.drugSku.id) FROM DrugBatch b JOIN b.shelf s " +
                         "WHERE b.quantityOnHand > 0 AND b.isActive = true")
            .pageSize(chunkSize)
            .saveState(true)
            .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<WardSkuPair, AiForecastReport> forecastProcessor() {
        return pair -> {
            try {
                return forecastService.generateForecast(pair.wardId(), pair.skuId());
            } catch (Exception e) {
                log.error("Failed to generate forecast for ward={}, sku={}: {}",
                    pair.wardId(), pair.skuId(), e.getMessage());
                return null;
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<AiForecastReport> forecastWriter() {
        return chunk -> {
            var items = chunk.getItems().stream()
                .filter(java.util.Objects::nonNull)
                .toList();
            if (!items.isEmpty()) {
                forecastPersistenceService.saveAll(items);
            }
            log.info("AiForecastBatchJob: processed {} forecasts", items.size());
        };
    }
}
