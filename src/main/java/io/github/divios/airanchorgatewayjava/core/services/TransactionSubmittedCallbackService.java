package io.github.divios.airanchorgatewayjava.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import io.github.divios.airanchorgatewayjava.core.mappers.Payload2ModelMapper;
import io.github.divios.airanchorgatewayjava.core.pojos.TransactionPayloadWrapper;
import io.github.divios.airanchorgatewayjava.core.utils.CryptoUtils;
import io.github.divios.airanchorgatewayjava.data.dao.LocationsDAO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TransactionSubmittedCallbackService {

    private static final ExecutorService executor = Executors.newCachedThreadPool();

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Payload2ModelMapper payload2ModelMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private LocationsDAO locationsDAO;

    @SneakyThrows
    public void startCallback(List<TransactionPayloadWrapper> transactions) {
        for (var transaction : transactions) {
            var hash = getHash(transaction);
            var channel = transaction.getChannel();
            var tag = transaction.getTag();

            createQueue(channel, hash);

            executor.submit(() -> receiveOnQueue(transaction, hash, channel, tag));
        }
    }

    @SneakyThrows
    private void receiveOnQueue(TransactionPayloadWrapper payload, String hash, Channel channel, long tag) {
        log.info("Waiting for validation for transaction with hash {}", hash);
        Message response = rabbitTemplate.receive(hash, TimeUnit.SECONDS.toMillis(10));

        if (response != null) {
            log.info("Transaction with hash: {}, was confirmed", hash);
            channel.basicAck(tag, false);
            locationsDAO.save(payload2ModelMapper.map(payload.getPayload()));
            channel.queueDelete(hash);

        } else {
            log.error("Transaction with hash: {}, was not confirmed, requeue...", hash);
            channel.basicReject(tag, true);
        }
    }

    private String getHash(TransactionPayloadWrapper transaction) throws JsonProcessingException {
        return cryptoUtils.hash(objectMapper.writeValueAsString(transaction.getPayload()));
    }

    @SneakyThrows
    private void createQueue(Channel channel, String hash) {
        channel.queueDeclare(hash, false, false, false, Map.of());
    }


}
