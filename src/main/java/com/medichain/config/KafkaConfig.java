package com.medichain.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String STOCK_ALERTS_THRESHOLD = "stock.alerts.threshold";
    public static final String EXPIRY_ALERTS_WARNING = "expiry.alerts.warning";
    public static final String EXPIRY_ALERTS_CRITICAL = "expiry.alerts.critical";
    public static final String PROCUREMENT_ORDER_APPROVED = "procurement.order.approved";
    public static final String NGO_TRANSFER_COMPLETED = "ngo.transfer.completed";
    public static final String AI_FORECAST_COMPLETED = "ai.forecast.completed";

    @Bean
    public NewTopic stockAlertsThreshold() {
        return TopicBuilder.name(STOCK_ALERTS_THRESHOLD)
            .partitions(3)
            .replicas(1)
            .compact()
            .build();
    }

    @Bean
    public NewTopic expiryAlertsWarning() {
        return TopicBuilder.name(EXPIRY_ALERTS_WARNING)
            .partitions(2)
            .replicas(1)
            .compact()
            .build();
    }

    @Bean
    public NewTopic expiryAlertsCritical() {
        return TopicBuilder.name(EXPIRY_ALERTS_CRITICAL)
            .partitions(2)
            .replicas(1)
            .compact()
            .build();
    }

    @Bean
    public NewTopic procurementOrderApproved() {
        return TopicBuilder.name(PROCUREMENT_ORDER_APPROVED)
            .partitions(2)
            .replicas(1)
            .build();
    }

    @Bean
    public NewTopic ngoTransferCompleted() {
        return TopicBuilder.name(NGO_TRANSFER_COMPLETED)
            .partitions(2)
            .replicas(1)
            .compact()
            .build();
    }

    @Bean
    public NewTopic aiForecastCompleted() {
        return TopicBuilder.name(AI_FORECAST_COMPLETED)
            .partitions(3)
            .replicas(1)
            .compact()
            .build();
    }
}
