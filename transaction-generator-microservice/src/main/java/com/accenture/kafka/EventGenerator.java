package net.renalias.kafka;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventGenerator {

    private static final Logger log = LoggerFactory.getLogger(EventGenerator.class);

    @Autowired
    private KafkaTemplate<String, TransactionEvent> template;

    private TransactionEvent generateTransactionEvent() {
        TransactionEvent transactionEvent = new TransactionEvent();
        transactionEvent.setId(UUID.randomUUID().toString());
        transactionEvent.setAccount_from("account-1");
        transactionEvent.setAccount_to("account-2");
        transactionEvent.setAmount(123);
        transactionEvent.setCustomer_id("customer-1");

        return(transactionEvent);
    }

    @Scheduled(fixedRate = 4000)
    public void publishEvent() {
        log.info("Pushing messages");
        this.template.send("transactions-feed", generateTransactionEvent());
		this.template.send("transactions-feed", generateTransactionEvent());
        this.template.send("transactions-feed", generateTransactionEvent());		
    }
}