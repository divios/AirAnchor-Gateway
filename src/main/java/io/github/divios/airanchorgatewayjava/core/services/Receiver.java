package io.github.divios.airanchorgatewayjava.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import io.github.airanchordtos.TransactionRequest;
import io.github.divios.airanchorgatewayjava.core.pojos.BatchTransactionRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Receiver {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BatchBlockingQueueService batchBlockingQueueService;

    @RabbitListener(queues = "gateway_queue", ackMode = "MANUAL")
    @SneakyThrows
    public void handleRequests(String message, Channel channel,
                               @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        log.info("Received: {}", message);

        var request = unmarshall(message);
        if (request == null)
            return;

        var batchRequest = BatchTransactionRequest.of(channel, tag, request);
        batchBlockingQueueService.submit(batchRequest);

        //channel.basicNack(tag, false, false);
    }

    private TransactionRequest unmarshall(String message) {
        try {
            return objectMapper.readValue(message, TransactionRequest.class);
        } catch (Exception e) {
            log.error("Invalid request, could not deserialize from json");
            return null;
        }
    }

}
