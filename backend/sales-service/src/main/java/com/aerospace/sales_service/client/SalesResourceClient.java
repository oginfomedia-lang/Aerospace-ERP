package com.aerospace.sales_service.client;

import com.aerospace.sales_service.client.dto.PartStockInfo;
import com.aerospace.sales_service.common.ApiResponse;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

// sales-service's telephone — the only class allowed to actually dial another
// microservice. Forwards the caller's own JWT so the target service's normal
// @PreAuthorize checks apply exactly as if the logged-in user called it directly.
// Same shape as production-service's ProductionResourceClient.
@Component
@RequiredArgsConstructor
@Slf4j
public class SalesResourceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${services.inventory.base-url}")
    private String inventoryBaseUrl;
    @Value("${services.production.base-url}")
    private String productionBaseUrl;

    // GET — safe to retry on a connect/read timeout, nothing is mutated downstream.
    @Retry(name = "inventoryService")
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "getPartStockFallback")
    public PartStockInfo getPartStock(Long partId) {
        URI uri = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/" + partId).build().toUri();
        return exchange(uri, HttpMethod.GET,
                new ParameterizedTypeReference<ApiResponse<PartStockInfo>>() {}).getData();
    }

    private PartStockInfo getPartStockFallback(Long partId, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    // GET — safe to retry.
    @Retry(name = "productionService")
    @CircuitBreaker(name = "productionService", fallbackMethod = "getPlannedProductionQuantityFallback")
    public Integer getPlannedProductionQuantity(String productCode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(productionBaseUrl + "/production/planned-quantity")
                .queryParam("productCode", productCode).build().toUri();
        Integer qty = exchange(uri, HttpMethod.GET,
                new ParameterizedTypeReference<ApiResponse<Integer>>() {}).getData();
        return qty != null ? qty : 0;
    }

    private Integer getPlannedProductionQuantityFallback(String productCode, Throwable ex) {
        throw new IllegalStateException("Production service is currently unavailable: " + ex.getMessage());
    }

    // Reserves a specific batch's APPROVED stock (moves it to RESERVED) — the exact
    // same existing endpoint inventory-service's own Store Keeper screen already uses
    // for Work Order material reservation, just called here on behalf of a Sales Order.
    // POST with a real side effect and no dedup guard on inventory-service's side
    // (PartServiceImpl.reserveStock has none) — circuit breaker only, no @Retry: a
    // retried timeout could double-reserve stock.
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "reserveStockFallback")
    public void reserveStock(Long partId, String batchNumber, int quantity) {
        URI uri = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/" + partId + "/reserve")
                .queryParam("batch", batchNumber).queryParam("quantity", quantity).build().toUri();
        exchange(uri, HttpMethod.POST, new ParameterizedTypeReference<ApiResponse<String>>() {});
    }

    private void reserveStockFallback(Long partId, String batchNumber, int quantity, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    // Same reasoning as reserveStock — no dedup guard on releaseReservation either.
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "releaseReservationFallback")
    public void releaseReservation(Long partId, String batchNumber, int quantity) {
        URI uri = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/" + partId + "/release-reservation")
                .queryParam("batch", batchNumber).queryParam("quantity", quantity).build().toUri();
        exchange(uri, HttpMethod.POST, new ParameterizedTypeReference<ApiResponse<String>>() {});
    }

    private void releaseReservationFallback(Long partId, String batchNumber, int quantity, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    // THE moment real stock actually leaves the building (dispatch confirmed) —
    // permanently removes previously-RESERVED stock, unlike reserve/release which just
    // move it between buckets. Same non-idempotency caveat as order-service's
    // issueMaterial: no dedup guard downstream, so no @Retry — a retried timeout could
    // double-consume stock that already left the building.
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "consumeStockFallback")
    public void consumeStock(Long partId, String batchNumber, int quantity) {
        URI uri = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/" + partId + "/consume-reserved")
                .queryParam("batch", batchNumber).queryParam("quantity", quantity).build().toUri();
        exchange(uri, HttpMethod.POST, new ParameterizedTypeReference<ApiResponse<String>>() {});
    }

    private void consumeStockFallback(Long partId, String batchNumber, int quantity, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    // Receives a customer return into inventory — the exact same GRN receipt endpoint
    // used for supplier deliveries, just with the RMA number as the traceability
    // reference. Lands as PENDING_QA (or straight to APPROVED if the part doesn't
    // require QC) — inventory-service decides that, not the caller. Returns the
    // system-generated batch number so the RMA can remember which batch to inspect later.
    // POST that creates a new stock receipt each call, no dedup key — circuit breaker
    // only, a retried timeout could create a duplicate GRN receipt.
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "receiveReturnStockFallback")
    public PartStockInfo receiveReturnStock(Long partId, int quantity, String location, String referenceNote) {
        URI uri = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/" + partId + "/inward-from-grn")
                .queryParam("qty", quantity).queryParam("location", location)
                .queryParam("challanNumber", referenceNote).build().toUri();
        return exchange(uri, HttpMethod.POST, new ParameterizedTypeReference<ApiResponse<PartStockInfo>>() {}).getData();
    }

    private PartStockInfo receiveReturnStockFallback(Long partId, int quantity, String location,
                                                       String referenceNote, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    // The RMA inspection decision, released through inventory-service's normal QA
    // gate — same "PENDING_QA -> APPROVED/REJECTED" lifecycle every other receipt goes
    // through, just triggered by a return instead of a GRN. A retried qa-decision after
    // the first one already applied would 404 (the batch is no longer PENDING_QA) —
    // circuit breaker only, no @Retry.
    @CircuitBreaker(name = "inventoryService", fallbackMethod = "approveReturnedStockFallback")
    public void approveReturnedStock(Long partId, String batchNumber, String remarks) {
        qaDecision(partId, batchNumber, "APPROVED", remarks != null ? remarks : "Return accepted");
    }

    private void approveReturnedStockFallback(Long partId, String batchNumber, String remarks, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    @CircuitBreaker(name = "inventoryService", fallbackMethod = "rejectReturnedStockFallback")
    public void rejectReturnedStock(Long partId, String batchNumber, String remarks) {
        qaDecision(partId, batchNumber, "REJECTED", remarks != null ? remarks : "Return rejected");
    }

    private void rejectReturnedStockFallback(Long partId, String batchNumber, String remarks, Throwable ex) {
        throw new IllegalStateException("Inventory service is currently unavailable: " + ex.getMessage());
    }

    private void qaDecision(Long partId, String batchNumber, String status, String remarks) {
        URI uri = UriComponentsBuilder.fromHttpUrl(inventoryBaseUrl + "/inventory/" + partId + "/qa-decision")
                .queryParam("batchNumber", batchNumber).queryParam("status", status)
                .queryParam("remarks", remarks).build().toUri();
        exchange(uri, HttpMethod.POST, new ParameterizedTypeReference<ApiResponse<PartStockInfo>>() {});
    }

    // Takes a fully-built URI (never a raw String) — UriComponentsBuilder above is the
    // ONLY place query values get percent-encoded, exactly once. Passing a String here
    // instead would let RestTemplate's own URI-template expansion re-encode an
    // already-encoded value (e.g. a batch number containing "/") a second time,
    // corrupting it into something that matches nothing server-side.
    private <T> T exchange(URI uri, HttpMethod method, ParameterizedTypeReference<T> type) {
        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        try {
            return restTemplate.exchange(uri, method, new HttpEntity<>(headers), type).getBody();
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(ex.getResponseBodyAsString());
        }
    }
}
