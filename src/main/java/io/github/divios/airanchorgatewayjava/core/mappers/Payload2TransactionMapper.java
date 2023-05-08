package io.github.divios.airanchorgatewayjava.core.mappers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.ByteString;
import io.github.airanchordtos.TransactionPayload;
import io.github.divios.airanchorgatewayjava.core.services.KeyWrapperService;
import io.github.divios.airanchorgatewayjava.core.utils.CryptoUtils;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import sawtooth.sdk.protobuf.Transaction;
import sawtooth.sdk.protobuf.TransactionHeader;

import java.util.Arrays;
import java.util.List;

@Component
public class Payload2TransactionMapper {

    @Value("${family.name}")
    private String familyName;

    @Value("${family.version}")
    private String familyVersion;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KeyWrapperService keyWrapperService;

    @Autowired
    private CryptoUtils cryptoUtils;

    @SneakyThrows
    public Transaction map(TransactionPayload payload) {
        var header = getHeader(payload);
        var signature = keyWrapperService.firm(header.toByteArray());

        return Transaction.newBuilder()
                .setHeader(header.toByteString())
                .setHeaderSignature(signature)
                .setPayload(ByteString.copyFrom(objectMapper.writeValueAsBytes(payload)))
                .build();
    }

    @SneakyThrows
    private TransactionHeader getHeader(TransactionPayload payload) {
        var payloadHash = cryptoUtils.hash(objectMapper.writeValueAsString(payload));
        var senderKey = payload.getCertificate_request().getHeader().getSender_public_key();

        var address = cryptoUtils.makeLocationKeyAddress(senderKey, payloadHash);

        return TransactionHeader.newBuilder()
                .setSignerPublicKey(keyWrapperService.getPubKey())
                .setFamilyName(familyName)
                .setFamilyVersion(familyVersion)
                .addAllInputs(List.of(address))
                .addAllOutputs(List.of(address))
                .addAllDependencies(List.of())
                .setPayloadSha512(payloadHash)
                .setBatcherPublicKey(keyWrapperService.getPubKey())
                .setNonce(cryptoUtils.generateNonce())
                .build();
    }

}
