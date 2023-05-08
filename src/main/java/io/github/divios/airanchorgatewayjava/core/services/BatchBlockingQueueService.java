package io.github.divios.airanchorgatewayjava.core.services;

import io.github.divios.airanchorgatewayjava.core.pojos.BatchTransactionRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class BatchBlockingQueueService {

    @Autowired
    private BatherProcessorService batherProcessorService;

    private final Queue<BatchTransactionRequest> requestsQueue =
            new ConcurrentLinkedDeque<>();

    public void submit(BatchTransactionRequest request) {
        requestsQueue.add(request);
    }

    @Async
    @Scheduled(fixedRate = 100L, initialDelay = 2000L)
    void cycler() {
        List<BatchTransactionRequest> requests = new LinkedList<>();

        BatchTransactionRequest batchRequest;
        while((batchRequest = requestsQueue.poll()) != null)
            requests.add(batchRequest);

        if (!requests.isEmpty())
            batherProcessorService.processBatch(requests);
    }

}
