package io.github.divios.airanchorgatewayjava.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.databind.CBORMapper;
import com.google.common.base.Preconditions;
import io.github.airanchordtos.CertificateResponse;
import io.github.airanchordtos.TransactionRequest;
import io.github.divios.airanchorgatewayjava.exceptions.InvalidRequestException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.Secp256k1PublicKey;

@Slf4j
@Service
public class MessageProcessorService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private CBORMapper cborMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Context context;

    @SneakyThrows
    public CertificateResponse process(TransactionRequest request)
            throws InvalidRequestException, IllegalStateException {
        validateRequest(request);

        return getCertificateResponse(request);
    }

    private void validateRequest(TransactionRequest request) {
        Secp256k1PublicKey pubKey;
        try {
            pubKey = Secp256k1PublicKey.fromHex(request.getHeader().getSender_public_key());

        } catch (Exception e) {
            throw new InvalidRequestException("Invalid request pub key " +
                    request.getHeader().getSender_public_key());
        }

        try {
            var result = context.verify(request.getSignature(),
                    objectMapper.writeValueAsBytes(request.getHeader()), pubKey);

            Preconditions.checkArgument(result);

        } catch (Exception e) {
            throw new InvalidRequestException("Invalid signature " + request.getSignature());
        }
    }

    private CertificateResponse getCertificateResponse(TransactionRequest request) {
        var response = (CertificateResponse) rabbitTemplate.convertSendAndReceive("ca_queue",
                request.getHeader().getCertificate_request());

        if (response == null)
            throw new IllegalStateException("ca did not respond...");

        if (response.getSignature() == null && response.getCa_pub_key() == null)
            throw new InvalidRequestException("Ca marked request as invalid, skipping...");

        return response;
    }

}
