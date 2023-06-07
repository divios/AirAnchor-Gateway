package io.github.divios.airanchorgatewayjava.core.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;
import io.github.divios.airanchorgatewayjava.core.mappers.Payload2ModelMapper;
import io.github.divios.airanchorgatewayjava.core.pojos.TransactionPayloadWrapper;
import io.github.divios.airanchorgatewayjava.core.utils.CryptoUtils;
import io.github.divios.airanchorgatewayjava.data.dao.LocationsDAO;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class TransactionSubmittedCallbackService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Payload2ModelMapper payload2ModelMapper;

    @Autowired
    private CryptoUtils cryptoUtils;

    @Autowired
    private LocationsDAO locationsDAO;

    @Value("${transaction.confirm.wait.time}")
    private int expireTime;

    private Cache<String, TransactionPayloadWrapper> cache;

    @PostConstruct
    private void init() {
        cache = Caffeine.newBuilder()
                .expireAfterWrite(expireTime, TimeUnit.MINUTES)
                .removalListener(this::removalListener)
                .build();
    }

    @SneakyThrows
    public void startCallback(List<TransactionPayloadWrapper> transactions) {
        for (var transaction : transactions) {
            var hash = getHash(transaction);

            log.info("Waiting for validation for transaction with hash {}", hash);
            cache.put(hash, transaction);
        }
    }

    @SneakyThrows
    @RabbitListener(queues = "gateway_callback_queue")
    private void receiveOnQueue(String hash) {

        var response = cache.asMap().get(hash);
        if (response == null)
            return;

        var channel = response.getChannel();
        var tag = response.getTag();

        log.info("Transaction with hash: {}, was confirmed", hash);

        channel.basicAck(tag, false);
        locationsDAO.save(payload2ModelMapper.map(response.getPayload()));
        cache.invalidate(hash);
    }

    private String getHash(TransactionPayloadWrapper transaction) throws JsonProcessingException {
        return cryptoUtils.hash(objectMapper.writeValueAsString(transaction.getPayload()));
    }

    @SneakyThrows
    private void removalListener(Object key, Object value, RemovalCause cause) {
        if (!cause.wasEvicted()) return;

        var hash = (String) key;
        var payload = (TransactionPayloadWrapper) value;

        log.info("Transaction with hash: {}, was not confirmed", hash);
        payload.getChannel().basicReject(payload.getTag(), true);
    }

    @Scheduled(fixedDelay = 1000*10L)
    void cleanUpCacheTask() {
        cache.cleanUp();
    }

}
