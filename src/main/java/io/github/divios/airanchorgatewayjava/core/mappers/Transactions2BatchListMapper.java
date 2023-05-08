package io.github.divios.airanchorgatewayjava.core.mappers;

import io.github.divios.airanchorgatewayjava.core.services.KeyWrapperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import sawtooth.sdk.protobuf.Batch;
import sawtooth.sdk.protobuf.BatchHeader;
import sawtooth.sdk.protobuf.BatchList;
import sawtooth.sdk.protobuf.Transaction;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class Transactions2BatchListMapper {

    @Autowired
    private KeyWrapperService keyWrapperService;

    public BatchList map(List<Transaction> transactions) {
        var transactionSignatures = transactions.stream()
                .map(Transaction::getHeaderSignature)
                .collect(Collectors.toList());

        var batchHeader = BatchHeader.newBuilder()
                .setSignerPublicKey(keyWrapperService.getPubKey())
                .addAllTransactionIds(transactionSignatures)
                .build();

        var signature = keyWrapperService.firm(batchHeader.toByteArray());

        var batch = Batch.newBuilder()
                .setHeader(batchHeader.toByteString())
                .addAllTransactions(transactions)
                .setHeaderSignature(signature)
                .build();

        return BatchList.newBuilder()
                .addBatches(batch)
                .build();
    }

}
