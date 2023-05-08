package io.github.divios.airanchorgatewayjava.core.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import sawtooth.sdk.signing.Context;
import sawtooth.sdk.signing.Secp256k1PrivateKey;

@Slf4j
@Service
public class KeyWrapperService {

    @Value("${gateway.private.key}")
    private String privateKey;

    @Autowired
    private Context context;

    @Autowired
    private ObjectMapper objectMapper;

    private Secp256k1PrivateKey secp256k1PrivateKey;

    @PostConstruct
    private void init() {
        try {
            secp256k1PrivateKey = Secp256k1PrivateKey.fromHex(privateKey);
        } catch (Exception e) {
            log.error("There was an error parsing the gateway private key. It it correct? " + e);
            System.exit(-1);
        }
    }

    @SneakyThrows
    public String firm(Object o) {
        return firm(objectMapper.writeValueAsBytes(o));
    }

    public String firm(String data) {
        return firm(data.getBytes());
    }

    public String firm(byte[] data) {
        return context.sign(data, secp256k1PrivateKey);
    }

    @Cacheable("gateway_pub_key")
    public String getPubKey() {
        return context.getPublicKey(secp256k1PrivateKey).hex();
    }

}
