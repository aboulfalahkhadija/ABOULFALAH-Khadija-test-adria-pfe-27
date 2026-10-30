package ma.enset.adria.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.enset.adria.transaction.domain.Transaction;
import ma.enset.adria.transaction.domain.TransactionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class TransactionEventProducer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventProducer.class);

    @Value("${bank.kafka.topic.transactions-created:bank.transactions.created}")
    private String topicName;

    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public TransactionEventProducer(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public TransactionCreatedEvent publishTransactionCreated(Transaction transaction) {
        TransactionCreatedEvent event = TransactionCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .eventType("TransactionCreated")
                .transactionId(transaction.getId())
                .accountId(transaction.getAccountId())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .occurredAt(transaction.getCreatedAt())
                .build();

        String messageKey = transaction.getAccountId().toString();

        try {
            String jsonPayload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
            log.info("[KAFKA PRODUCER] Publication dans le topic '{}' | Key (accountId)='{}' | Payload :\n{}",
                    topicName, messageKey, jsonPayload);
        } catch (Exception e) {
            log.info("[KAFKA PRODUCER] Publication dans le topic '{}' | Key (accountId)='{}' | Event={}",
                    topicName, messageKey, event);
        }

        // Diffusion asynchrone au consumer du topic bank.transactions.created
        eventPublisher.publishEvent(event);
        return event;
    }
}
