package io.github.divios.airanchorgatewayjava.core.mappers;

import io.github.airanchordtos.CertificateResponse;
import io.github.airanchordtos.TransactionPayload;
import io.github.airanchordtos.TransactionRequest;
import io.github.divios.airanchorgatewayjava.core.services.KeyWrapperService;
import io.github.divios.airanchorgatewayjava.core.utils.CryptoUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Transaction2PayloadMapper {

    @Autowired
    private KeyWrapperService keyWrapperService;

    @Autowired
    private CryptoUtils cryptoUtils;

    public TransactionPayload map(TransactionRequest request, CertificateResponse response) {
        return TransactionPayload.builder()
                .batcher_public_key(keyWrapperService.getPubKey())
                .certificate_authority_response(response)
                .certificate_request(request.getHeader().getCertificate_request())
                .nonce(cryptoUtils.generateNonce())
                .data(request.getData())
                .build();
    }

}
