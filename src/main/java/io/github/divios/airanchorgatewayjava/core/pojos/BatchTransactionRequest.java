package io.github.divios.airanchorgatewayjava.core.pojos;

import com.rabbitmq.client.Channel;
import io.github.airanchordtos.TransactionRequest;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BatchTransactionRequest {

    public static BatchTransactionRequest of(Channel channel,
                                             long tag,
                                             TransactionRequest request) {
        return new BatchTransactionRequest(channel, tag, request);
    }

    private Channel channel;
    private long tag;
    private TransactionRequest request;

}
