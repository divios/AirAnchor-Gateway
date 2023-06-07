package io.github.divios.airanchorgatewayjava.core.services;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BlockingStrategy;
import io.github.bucket4j.Bucket;
import io.github.divios.airanchorgatewayjava.core.pojos.BatchTransactionRequest;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
public class BatchBlockingQueueService {

    @Autowired
    private BatherProcessorService batherProcessorService;

    @Value("${token.bucket.limit.seconds}")
    private int tokenSecondsLimit;

    @Value("${token.bucket.limit.minutes}")
    private int tokenMinutesLimit;

    @Value("${token.bucket.initial.tokens}")
    private int initialTokens;

    private Queue<BatchTransactionRequest> requestsQueue;
    private Bucket bucket;

    @PostConstruct
    private void init() {
        requestsQueue = new ConcurrentLinkedDeque<>();
        bucket = Bucket.builder()
                .addLimit(Bandwidth.simple(tokenMinutesLimit, Duration.ofMinutes(1)))
                .addLimit(Bandwidth.simple(tokenSecondsLimit, Duration.ofSeconds(1))
                        .withInitialTokens(initialTokens))
                .build();
    }

    public void submit(BatchTransactionRequest request) {
        requestsQueue.add(request);
    }

    @SneakyThrows
    @Async
    @Scheduled(fixedDelay = 100L, initialDelay = 200L)
    void cycler() {
        List<BatchTransactionRequest> requests = new LinkedList<>();

        //log.info("Queue is {}", requestsQueue.size());
        int toConsume;
        if ((toConsume = requestsQueue.size()) == 0)
            return;

        //log.info("Waiting for blocking");
        bucket.asBlocking().consume(toConsume, BlockingStrategy.PARKING);
        //log.info("Now we are good");

        BatchTransactionRequest batchRequest;
        int i = 0;
        while ((++i <= toConsume) && (batchRequest = requestsQueue.poll()) != null)
            requests.add(batchRequest);

        if (!requests.isEmpty())
            batherProcessorService.processBatch(requests);
    }

}
