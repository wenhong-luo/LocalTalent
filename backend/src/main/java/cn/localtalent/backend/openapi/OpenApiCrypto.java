package cn.localtalent.backend.openapi;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class OpenApiCrypto {

    private OpenApiCrypto() {
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("failed to hash open api payload", exception);
        }
    }

    public static String hmacSha256Hex(String signingString, String secretHashHex) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            byte[] secretDigest = HexFormat.of().parseHex(secretHashHex);
            mac.init(new SecretKeySpec(secretDigest, "HmacSHA256"));
            byte[] digest = mac.doFinal(signingString.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("failed to sign open api request", exception);
        }
    }
}
