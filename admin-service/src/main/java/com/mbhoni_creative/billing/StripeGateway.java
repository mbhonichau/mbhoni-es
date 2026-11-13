package com.mbhoni_creative.billing;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.http.HttpClient;
import java.time.Duration;

@Component
public class StripeGateway {
    private final RestClient client;
    private final String apiKey;
    public StripeGateway(@Value("${app.stripe.secret-key:}") String apiKey) {
        this.apiKey = apiKey;
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        client = RestClient.builder().baseUrl("https://api.stripe.com/v1")
                .defaultHeader("Stripe-Version", "2025-06-30.basil")
                .requestFactory(factory).defaultHeader("Authorization", "Bearer " + apiKey).build();
    }
    public JsonNode subscription(String id) {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe API is not configured");
        if (id == null || !id.matches("sub_[A-Za-z0-9]+")) throw new IllegalArgumentException("Invalid Stripe subscription ID");
        return client.get().uri(uri -> uri.path("/subscriptions/{id}")
                .queryParam("expand[]", "default_payment_method")
                .queryParam("expand[]", "customer.invoice_settings.default_payment_method")
                .queryParam("expand[]", "latest_invoice").build(id)).retrieve().body(JsonNode.class);
    }
    public static String id(JsonNode node) { return node.isTextual() ? node.asText() : node.path("id").asText(""); }
    public JsonNode invoices(String customer, String subscription, String after) {
        if (apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe API is not configured");
        if (!customer.matches("cus_[A-Za-z0-9]+") || !subscription.matches("sub_[A-Za-z0-9]+"))
            throw new IllegalArgumentException("Verified Stripe mapping required");
        return client.get().uri(uri -> {
            var builder = uri.path("/invoices").queryParam("customer", customer).queryParam("subscription", subscription).queryParam("limit", 100);
            if (after != null) builder.queryParam("starting_after", after);
            return builder.build();
        }).retrieve().body(JsonNode.class);
    }
    public static boolean hasCard(JsonNode subscription) {
        JsonNode method = subscription.path("default_payment_method");
        if (!method.isObject()) method = subscription.path("customer").path("invoice_settings").path("default_payment_method");
        return "card".equals(method.path("type").asText());
    }
    public String portal(String customerId, String returnUrl, String configuration) {
        if (apiKey.isBlank() || configuration == null || configuration.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe portal is not configured");
        if (customerId == null || !customerId.matches("cus_[A-Za-z0-9]+")) throw new IllegalArgumentException("Stripe customer required");
        java.net.URI destination = java.net.URI.create(returnUrl);
        if (!"https".equals(destination.getScheme()) || destination.getHost() == null)
            throw new IllegalArgumentException("Configured portal return URL must use HTTPS");
        org.springframework.util.MultiValueMap<String, String> form = new org.springframework.util.LinkedMultiValueMap<>();
        form.add("customer", customerId); form.add("return_url", returnUrl); form.add("configuration", configuration);
        JsonNode session = client.post().uri("/billing_portal/sessions").contentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED)
                .body(form).retrieve().body(JsonNode.class);
        if (session == null) throw new IllegalStateException("Stripe returned no portal session");
        String url = session.path("url").asText(); java.net.URI portal = java.net.URI.create(url);
        if (!"https".equals(portal.getScheme()) || !"billing.stripe.com".equals(portal.getHost()))
            throw new IllegalStateException("Unexpected Stripe portal URL");
        return url;
    }
}
