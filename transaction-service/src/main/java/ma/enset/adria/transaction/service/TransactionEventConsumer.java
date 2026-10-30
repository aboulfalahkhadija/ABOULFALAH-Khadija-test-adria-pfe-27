package ma.enset.adria.transaction.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ma.enset.adria.transaction.domain.TransactionCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);

    @Value("${bank.kafka.topic.transactions-created:bank.transactions.created}")
    private String topicName;

    private final List<TransactionCreatedEvent> consumedEvents = Collections.synchronizedList(new ArrayList<>());
    private final ObjectMapper objectMapper;

    public TransactionEventConsumer() {
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Consumer de l'événement publié dans le topic bank.transactions.created.
     * Journalise l'événement reçu sans modifier les données bancaires.
     */
    @EventListener
    public void onTransactionCreatedEvent(TransactionCreatedEvent event) {
        consumedEvents.add(event);
        String key = event.getAccountId() != null ? event.getAccountId().toString() : "null";
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(event);
            log.info("[KAFKA CONSUMER] Message reçu depuis le topic '{}' | Key (accountId)='{}' | Sans modification des données bancaires :\n{}",
                    topicName, key, json);
        } catch (Exception e) {
            log.info("[KAFKA CONSUMER] Message reçu depuis le topic '{}' | Key (accountId)='{}' | Event={}",
                    topicName, key, event);
        }
    }

    public List<TransactionCreatedEvent> getConsumedEvents() {
        return List.copyOf(consumedEvents);
    }
}
