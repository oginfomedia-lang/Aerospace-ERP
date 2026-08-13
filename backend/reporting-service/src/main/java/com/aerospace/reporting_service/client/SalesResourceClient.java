package com.aerospace.reporting_service.client;

import com.aerospace.reporting_service.client.dto.SalesReportSummaryDTO;
import com.aerospace.reporting_service.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// reporting-service's telephone to sales-service — forwards the logged-in user's own
// JWT, so sales-service's @PreAuthorize on /reports/summary applies exactly as if
// they'd called it directly. Same shape as sales-service's own SalesResourceClient.
@Component
@RequiredArgsConstructor
public class SalesResourceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${services.sales.base-url}")
    private String salesBaseUrl;

    public SalesReportSummaryDTO getSummary(int days) {
        URI uri = UriComponentsBuilder.fromHttpUrl(salesBaseUrl + "/sales/reports/summary")
                .queryParam("days", days).build().toUri();
        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        try {
            ApiResponse<SalesReportSummaryDTO> body = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<ApiResponse<SalesReportSummaryDTO>>() {}).getBody();
            return body != null ? body.getData() : null;
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(ex.getResponseBodyAsString());
        }
    }
}