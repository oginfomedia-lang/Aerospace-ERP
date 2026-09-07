package com.aerospace.auth_service.client;

import com.aerospace.auth_service.dto.SupplierLookupResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

// auth-service's telephone to procurement-service — forwards the logged-in user's own
// JWT so procurement-service's normal @PreAuthorize checks apply exactly as if they'd
// called it directly. Same shape as sales-service's SalesResourceClient / QaServiceClient.
//
// Pulled out of SupplierAuthService (rather than annotated in place there) because the
// original call was a private method invoked via self-invocation from
// inviteSupplier() — Spring AOP proxies (which @Retry/@CircuitBreaker rely on) don't
// intercept calls a bean makes to itself, so the annotations would silently never fire
// if left on a private method in that class. Routing the call through this separate
// bean means it always crosses the proxy.
@Component
@RequiredArgsConstructor
public class ProcurementServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${services.procurement.base-url}")
    private String procurementBaseUrl;

    // GET — safe to retry on a connect/read timeout, nothing is mutated downstream.
    @Retry(name = "procurementService")
    @CircuitBreaker(name = "procurementService", fallbackMethod = "getSupplierFallback")
    public SupplierLookupResponse getSupplier(Long supplierId) {
        String url = procurementBaseUrl + "/suppliers/" + supplierId;

        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }

        try {
            return restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    SupplierLookupResponse.class).getBody();
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException("Could not verify supplier confirmation status: " + ex.getStatusCode());
        }
    }

    private SupplierLookupResponse getSupplierFallback(Long supplierId, Throwable ex) {
        throw new IllegalStateException("Procurement service is currently unavailable: " + ex.getMessage());
    }
}
