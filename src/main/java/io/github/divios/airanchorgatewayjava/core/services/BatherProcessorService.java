package io.github.divios.airanchorgatewayjava.core.services;

import io.github.airanchordtos.CertificateResponse;
import io.github.divios.airanchorgatewayjava.core.mappers.Payload2TransactionMapper;
import io.github.divios.airanchorgatewayjava.core.mappers.Transaction2PayloadMapper;
import io.github.divios.airanchorgatewayjava.core.mappers.Transactions2BatchListMapper;
import io.github.divios.airanchorgatewayjava.core.pojos.BatchTransactionRequest;
import io.github.divios.airanchorgatewayjava.exceptions.InvalidRequestException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import sawtooth.sdk.protobuf.ClientBatchSubmitResponse;
import sawtooth.sdk.protobuf.Transaction;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BatherProcessorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MessageProcessorService messageProcessorService;

    @Autowired
    private ZMQConnectionService zmqConnectionService;

    @Autowired
    private Transaction2PayloadMapper transaction2PayloadMapper;

    @Autowired
    private Payload2TransactionMapper payload2TransactionMapper;

    @Autowired
    private Transactions2BatchListMapper transactions2BatchListMapper;


    @SneakyThrows
    @Async
    public void processBatch(List<BatchTransactionRequest> requests) {
        log.info("Getting petition to process {}", requests);

        List<Transaction> transactions = mapRequestToTransactions(requests);

        var batchList = transactions2BatchListMapper.map(transactions);
        var responseFuture = zmqConnectionService.send(batchList);

        try {
            var response = responseFuture.getResult(5L);
            var batchResponse = ClientBatchSubmitResponse.parseFrom(response);

            if (batchResponse.getStatus() == ClientBatchSubmitResponse.Status.OK) {
                log.info("Resolved as ok");

                var last = requests.get(requests.size() - 1);
                last.getChannel().basicAck(last.getTag(), true);
            }

        } catch (Exception e) {
            var last = requests.get(requests.size() - 1);
            last.getChannel().basicNack(last.getTag(), true, true);
            e.printStackTrace();
        }
    }

    private List<Transaction> mapRequestToTransactions(List<BatchTransactionRequest> requests) {
        List<Transaction> transactions = new ArrayList<>(requests.size());

        for (var request : requests) {

            var certResponse = getCertificate(request);
            if (certResponse == null)
                continue;

            var payload = transaction2PayloadMapper
                    .map(request.getRequest(), certResponse);

            var tr = payload2TransactionMapper.map(payload);
            transactions.add(tr);
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
