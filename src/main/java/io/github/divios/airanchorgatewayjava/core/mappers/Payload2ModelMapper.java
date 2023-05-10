package io.github.divios.airanchorgatewayjava.core.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.airanchordtos.CertificateResponse;
import io.github.airanchordtos.TransactionPayload;
import io.github.divios.airanchorgatewayjava.core.services.KeyWrapperService;
import io.github.divios.airanchorgatewayjava.core.utils.CryptoUtils;
import io.github.divios.airanchorgatewayjava.data.model.TransactionModel;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Payload2ModelMapper {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeyWrapperService keyWrapperService;

    @Autowired
    private CryptoUtils cryptoUtils;

    @SneakyThrows
    public TransactionModel map(TransactionPayload transactionPayload) {
        return TransactionModel.builder()
                .sender(transactionPayload.getCertificate_request().getHeader().getSender_public_key())
                .ca(transactionPayload.getCertificate_authority_response().getCa_pub_key())
                .hash(cryptoUtils.hash(objectMapper.writeValueAsString(transactionPayload)))
                .signer(keyWrapperService.getPubKey())
                .build();
    }

}
