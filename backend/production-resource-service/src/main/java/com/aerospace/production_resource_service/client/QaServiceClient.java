package com.aerospace.production_resource_service.client;

import com.aerospace.production_resource_service.client.dto.EquipmentInfo;
import com.aerospace.production_resource_service.client.dto.ExternalApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

// Same synchronous-call pattern as production-service's ProductionResourceClient —
// forwards the caller's own JWT so qa-service's normal @PreAuthorize checks apply.
@Component
@RequiredArgsConstructor
@Slf4j
public class QaServiceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${services.qa.base-url}")
    private String baseUrl;

    // GET — safe to retry on a connect/read timeout.
    // Deliberately lets Spring's RestClientException (HTTP errors, connection
    // failures, timeouts) propagate unwrapped — the caller needs to tell "qa-service
    // is unreachable" (soft-fail, don't block on it) apart from its own business
    // decision once it HAS a real answer (hard-fail). Wrapping both into the same
    // exception type here would erase that distinction for every caller.
    @Retry(name = "qaService")
    @CircuitBreaker(name = "qaService", fallbackMethod = "getEquipmentFallback")
    public EquipmentInfo getEquipment(Long id) {
        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        ExternalApiResponse<EquipmentInfo> body = restTemplate.exchange(
                baseUrl + "/qa/calibration/equipment/" + id, HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<ExternalApiResponse<EquipmentInfo>>() {}).getBody();
        return body != null ? body.getData() : null;
    }

    // Keeps the same RestClientException-family contract as above — including when
    // the circuit is OPEN (CallNotPermittedException, not a RestClientException by
    // itself) — so MachineServiceImpl.occupyMachine's existing
    // `catch (RestClientException ex)` soft-fail still catches it instead of the
    // failure turning into an unintended hard block on machine assignment.
    private EquipmentInfo getEquipmentFallback(Long id, Throwable ex) {
        throw new ResourceAccessException("qa-service is currently unavailable: " + ex.getMessage());
    }
}
