package cn.localtalent.backend.interview.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class SigninCodeService {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode() {
        byte[] bytes = new byte[24];
        secureRandom.nextBytes(bytes);
        return "LT-" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hash(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
