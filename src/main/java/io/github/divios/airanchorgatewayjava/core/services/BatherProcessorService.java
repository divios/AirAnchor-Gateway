package io.github.divios.airanchorgatewayjava.core.services;

import io.github.airanchordtos.CertificateResponse;
import io.github.divios.airanchorgatewayjava.core.mappers.Payload2TransactionMapper;
import io.github.divios.airanchorgatewayjava.core.mappers.Transaction2PayloadMapper;
import io.github.divios.airanchorgatewayjava.core.mappers.Transactions2BatchListMapper;
import io.github.divios.airanchorgatewayjava.core.pojos.BatchTransactionRequest;
import io.github.divios.airanchorgatewayjava.core.pojos.TransactionPayloadWrapper;
import io.github.divios.airanchorgatewayjava.exceptions.InvalidRequestException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sawtooth.sdk.messaging.Future;
import sawtooth.sdk.protobuf.ClientBatchSubmitResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BatherProcessorService {

    @Autowired
    private MessageProcessorService messageProcessorService;

    @Autowired
    private TransactionSubmittedCallbackService transactionSubmittedCallbackService;

    @Autowired
    private ZMQConnectionService zmqConnectionService;

    @Autowired
    private Transaction2PayloadMapper transaction2PayloadMapper;

    @Autowired
    private Payload2TransactionMapper payload2TransactionMapper;

    @Autowired
    private Transactions2BatchListMapper transactions2BatchListMapper;


    @Async
    public void processBatch(List<BatchTransactionRequest> requests) {
        log.info("Getting petition to process {}", requests);

        var transactions = mapRequestToTransactions(requests);
        var batches = transactions.stream()
                .map(TransactionPayloadWrapper::getPayload)
                .map(payload2TransactionMapper::map)
                .collect(Collectors.toList());

        if (transactions.isEmpty())             // skip if null
            return;

        var batchList = transactions2BatchListMapper.map(batches);
        var responseFuture = zmqConnectionService.send(batchList);

        sendBatch(transactions, responseFuture);
    }

    @SneakyThrows
    private void sendBatch(List<TransactionPayloadWrapper> requests, Future responseFuture) {
        try {
            var response = responseFuture.getResult(10L);
            var batchResponse = ClientBatchSubmitResponse.parseFrom(response);

            if (batchResponse.getStatus() == ClientBatchSubmitResponse.Status.OK) {
                log.info("Resolved as ok");

                transactionSubmittedCallbackService.startCallback(requests);
            }

        } catch (Exception e) {
            var last = requests.get(requests.size() - 1);
            last.getChannel().basicNack(last.getTag(), true, true);
            e.printStackTrace();
        }
    }

    private List<TransactionPayloadWrapper> mapRequestToTransactions(List<BatchTransactionRequest> requests) {
        List<TransactionPayloadWrapper> transactions = new ArrayList<>(requests.size());

        for (var iterator = requests.iterator(); iterator.hasNext(); ) {
            var request = iterator.next();

            var certResponse = getCertificate(request);     // request certificate
            if (certResponse == null) {
                iterator.remove();          // remove from list if fails
                continue;
            }

            var payload = transaction2PayloadMapper
                    .map(request.getRequest(), certResponse);

            transactions.add(
                    new TransactionPayloadWrapper(payload, request.getChannel(), request.getTag())
            );
        }
        return transactions;
    }

    @SneakyThrows
    private CertificateResponse getCertificate(BatchTransactionRequest batchRequest) {
        CertificateResponse response = null;

        var channel = batchRequest.getChannel();
        var tag = batchRequest.getTag();

        try {
            response = messageProcessorService.process(batchRequest.getRequest());

        } catch (InvalidRequestException e) {
            log.error(e.getMessage());
            channel.basicReject(tag, false);            // Reject if invalid

        } catch (Exception e) {
            channel.basicReject(tag, true);             // requeue if something went wrong
        }

        return response;
    }

}
