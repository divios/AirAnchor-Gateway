package io.github.divios.airanchorgatewayjava.core.pojos;

import com.rabbitmq.client.Channel;
import io.github.airanchordtos.TransactionPayload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TransactionPayloadWrapper {

    private TransactionPayload payload;
    private Channel channel;
    private long tag;

}
