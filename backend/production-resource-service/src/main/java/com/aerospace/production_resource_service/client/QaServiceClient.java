package com.aerospace.production_resource_service.client;

import com.aerospace.production_resource_service.client.dto.EquipmentInfo;
import com.aerospace.production_resource_service.client.dto.ExternalApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
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

    // Deliberately lets Spring's RestClientException (HTTP errors, connection
    // failures, timeouts) propagate unwrapped — the caller needs to tell "qa-service
    // is unreachable" (soft-fail, don't block on it) apart from its own business
    // decision once it HAS a real answer (hard-fail). Wrapping both into the same
    // exception type here would erase that distinction for every caller.
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
}
