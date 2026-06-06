package com.medichain.messaging.producer;

import com.medichain.domain.entity.Enums.AlertType;
import com.medichain.domain.entity.StockAlert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StockAlertProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String STOCK_ALERTS_TOPIC = "stock.alerts.threshold";
    private static final String EXPIRY_ALERTS_TOPIC = "expiry.alerts.warning";

    public void publishStockAlert(StockAlert alert) {
        var topic = switch (alert.getAlertType()) {
            case EXPIRY_CRITICAL -> "expiry.alerts.critical";
            case EXPIRY_WARNING -> EXPIRY_ALERTS_TOPIC;
            default -> STOCK_ALERTS_TOPIC;
        };

        kafkaTemplate.send(topic, alert.getId().toString(), alert)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to send alert {} to topic {}: {}", alert.getId(), topic, ex.getMessage());
                } else {
                    log.debug("Alert {} published to {} at offset {}", alert.getId(), topic,
                        result.getRecordMetadata().offset());
                }
            });
    }
}
