package com.mbhoni_creative.billing;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class StripeSignatureVerifierTest {
    private final Instant now = Instant.parse("2026-09-26T12:00:00Z");
    private final byte[] body = "{\"id\":\"evt_123\"}".getBytes(StandardCharsets.UTF_8);
    private final StripeSignatureVerifier verifier = new StripeSignatureVerifier("whsec_test", Clock.fixed(now, ZoneOffset.UTC));
    private String signature(long timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        mac.update((timestamp + ".").getBytes(StandardCharsets.UTF_8));
        return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(mac.doFinal(body));
    }
    @Test void acceptsAuthenticRawBodyAndRotatingSignatures() throws Exception {
        verifier.verify(body, signature(now.getEpochSecond()) + ",v1=" + "0".repeat(64));
    }
    @Test void rejectsTamperedRawBody() throws Exception {
        String header = signature(now.getEpochSecond());
        assertThrows(ResponseStatusException.class, () -> verifier.verify("{}".getBytes(StandardCharsets.UTF_8), header));
    }
    @Test void rejectsExpiredAndFutureReplay() throws Exception {
        for (int seconds : new int[] {-301, 301}) {
            String header = signature(now.getEpochSecond() + seconds);
            assertThrows(ResponseStatusException.class, () -> verifier.verify(body, header));
        }
    }
    @Test void rejectsMissingMalformedAndDuplicateTimestamp() throws Exception {
        assertThrows(ResponseStatusException.class, () -> verifier.verify(body, null));
        assertThrows(ResponseStatusException.class, () -> verifier.verify(body, "t=garbage,v1=bad"));
        String header = signature(now.getEpochSecond()) + ",t=" + now.getEpochSecond();
        assertThrows(ResponseStatusException.class, () -> verifier.verify(body, header));
    }
    @Test void unconfiguredEndpointFailsClosed() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> new StripeSignatureVerifier("", Clock.systemUTC()).verify(body, "anything"));
        assertEquals(503, ex.getStatusCode().value());
    }
}
