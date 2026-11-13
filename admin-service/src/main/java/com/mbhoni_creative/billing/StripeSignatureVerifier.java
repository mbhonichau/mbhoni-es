package com.mbhoni_creative.billing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Component
public class StripeSignatureVerifier {
    private final String secret;
    private final Clock clock;
    public StripeSignatureVerifier(@Value("${app.stripe.webhook-secret:}") String secret, Clock clock) {
        this.secret = secret; this.clock = clock;
    }
    public void verify(byte[] body, String header) {
        if (secret.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe webhook is not configured");
        if (body.length > 1048576 || header == null) throw invalid();
        try {
            Long timestamp = null;
            List<String> signatures = new ArrayList<>();
            for (String part : header.split(",")) {
                String[] pair = part.trim().split("=", 2);
                if (pair.length != 2) continue;
                if (pair[0].equals("t")) {
                    if (timestamp != null) throw invalid();
                    timestamp = Long.parseLong(pair[1]);
                }
                if (pair[0].equals("v1")) signatures.add(pair[1]);
            }
            long now = clock.instant().getEpochSecond();
            if (timestamp == null || timestamp < now - 300 || timestamp > now + 300) throw invalid();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
            byte[] expected = mac.doFinal(body);
            for (String signature : signatures) {
                if (signature.matches("[a-fA-F0-9]{64}") && MessageDigest.isEqual(expected, HexFormat.of().parseHex(signature))) return;
            }
            throw invalid();
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw invalid(); }
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Stripe signature"); }
}
