package io.github.divios.airanchorgatewayjava.core.utils;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class CryptoUtils {

    @Value("${family.name}")
    private String familyName;

    @Value("${family.version}")
    private String familyVersion;

    private String familyNamePrefix;

    @PostConstruct
    private void init() {
       familyNamePrefix = hash(familyName).substring(0, 6);
    }

    public String generateNonce() {
        SecureRandom random = new SecureRandom();
        byte[] nonceBytes = new byte[16]; // 128 bits
        random.nextBytes(nonceBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(nonceBytes);
    }

    @SneakyThrows
    public String hash(String input) {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public String makeLocationKeyAddress(String key, String hash) {
        String locationKey = familyNamePrefix + key.substring(0, 6);

        if (hash != null)
            locationKey += hash.substring(hash.length() - 58);

        return locationKey;
    }

}
