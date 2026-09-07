package com.aerospace.production_service.client;

import com.aerospace.production_service.client.dto.EmployeeInfo;
import com.aerospace.production_service.client.dto.ExternalApiResponse;
import com.aerospace.production_service.client.dto.MachineInfo;
import com.aerospace.production_service.client.dto.ReservationConflictInfo;
import com.aerospace.production_service.client.dto.ToolingInfo;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

// The first-ever synchronous cross-service call in production-service — every other
// inter-service interaction here goes through Kafka. This one has to be synchronous
// because the whole point is to REJECT a stage start before it happens if the chosen
// machine/employee isn't eligible or is already busy elsewhere; an async event can't
// do that. Forwards the caller's own JWT so production-resource-service's normal
// @PreAuthorize checks apply exactly as if the user called it directly — same pattern
// order-service already uses for its own call into inventory-service.
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductionResourceClient {

    private final RestTemplate restTemplate;
    private final HttpServletRequest request;

    @Value("${services.production-resource.base-url}")
    private String baseUrl;

    // GET — safe to retry on a connect/read timeout.
    @Retry(name = "productionResourceService")
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "getMachineFallback")
    public MachineInfo getMachine(Long id) {
        return exchange(baseUrl + "/resources/machines/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<ExternalApiResponse<MachineInfo>>() {}).getData();
    }

    private MachineInfo getMachineFallback(Long id, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    @Retry(name = "productionResourceService")
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "getEmployeeFallback")
    public EmployeeInfo getEmployee(Long id) {
        return exchange(baseUrl + "/resources/employees/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<ExternalApiResponse<EmployeeInfo>>() {}).getData();
    }

    private EmployeeInfo getEmployeeFallback(Long id, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    @Retry(name = "productionResourceService")
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "getToolingFallback")
    public ToolingInfo getTooling(Long id) {
        return exchange(baseUrl + "/resources/tooling/" + id, HttpMethod.GET, null,
                new ParameterizedTypeReference<ExternalApiResponse<ToolingInfo>>() {}).getData();
    }

    private ToolingInfo getToolingFallback(Long id, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    // Null return = no conflict. "at" is computed server-side by
    // production-resource-service (never trusted from this caller) — see
    // MachineReservationServiceImpl.checkActiveConflict / EmployeeReservation's twin.
    // GET — safe to retry.
    @Retry(name = "productionResourceService")
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "getMachineReservationConflictFallback")
    public ReservationConflictInfo getMachineReservationConflict(Long machineId, Long excludeProductionOrderId) {
        String url = baseUrl + "/resources/machine-reservations/active-conflict?machineId=" + machineId
                + "&excludeProductionOrderId=" + excludeProductionOrderId;
        return exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<ExternalApiResponse<ReservationConflictInfo>>() {}).getData();
    }

    private ReservationConflictInfo getMachineReservationConflictFallback(
            Long machineId, Long excludeProductionOrderId, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    @Retry(name = "productionResourceService")
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "getEmployeeReservationConflictFallback")
    public ReservationConflictInfo getEmployeeReservationConflict(Long employeeId, Long excludeProductionOrderId) {
        String url = baseUrl + "/resources/employee-reservations/active-conflict?employeeId=" + employeeId
                + "&excludeProductionOrderId=" + excludeProductionOrderId;
        return exchange(url, HttpMethod.GET, null,
                new ParameterizedTypeReference<ExternalApiResponse<ReservationConflictInfo>>() {}).getData();
    }

    private ReservationConflictInfo getEmployeeReservationConflictFallback(
            Long employeeId, Long excludeProductionOrderId, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    // Blocking on purpose — if this fails, the stage must not start. POST with a real
    // side effect (machine occupancy state change) and no dedup guard downstream
    // (MachineServiceImpl.occupyMachine hard-checks current occupancy, it isn't
    // idempotent) — circuit breaker only, no @Retry.
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "occupyMachineFallback")
    public void occupyMachine(Long machineId, Long stageLogId) {
        exchange(baseUrl + "/resources/machines/" + machineId + "/occupy", HttpMethod.POST,
                Map.of("stageLogId", stageLogId),
                new ParameterizedTypeReference<ExternalApiResponse<MachineInfo>>() {});
    }

    private void occupyMachineFallback(Long machineId, Long stageLogId, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    // Same non-idempotency reasoning as occupyMachine — EmployeeServiceImpl.occupyEmployee
    // hard-checks current occupancy too.
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "occupyEmployeeFallback")
    public void occupyEmployee(Long employeeId, Long stageLogId) {
        exchange(baseUrl + "/resources/employees/" + employeeId + "/occupy", HttpMethod.POST,
                Map.of("stageLogId", stageLogId),
                new ParameterizedTypeReference<ExternalApiResponse<EmployeeInfo>>() {});
    }

    private void occupyEmployeeFallback(Long employeeId, Long stageLogId, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    // Same non-idempotency reasoning as occupyMachine — ToolingServiceImpl.occupyTooling
    // hard-checks current occupancy too.
    @CircuitBreaker(name = "productionResourceService", fallbackMethod = "occupyToolingFallback")
    public void occupyTooling(Long toolingId, Long stageLogId) {
        exchange(baseUrl + "/resources/tooling/" + toolingId + "/occupy", HttpMethod.POST,
                Map.of("stageLogId", stageLogId),
                new ParameterizedTypeReference<ExternalApiResponse<ToolingInfo>>() {});
    }

    private void occupyToolingFallback(Long toolingId, Long stageLogId, Throwable ex) {
        throw new IllegalStateException("production-resource-service is currently unavailable: " + ex.getMessage());
    }

    // Best-effort on purpose — a network blip here shouldn't block an operator from
    // completing real work. A stuck occupancy is recoverable via the supervisor's
    // Force Release button; a stage that can't be marked complete is not.
    // Deliberately NOT annotated with @Retry/@CircuitBreaker: the try/catch below
    // already swallows every failure inside this method, so no exception ever reaches
    // the resilience4j proxy for it to act on — the annotations would be dead weight.
    public void releaseMachine(Long machineId) {
        try {
            exchange(baseUrl + "/resources/machines/" + machineId + "/release", HttpMethod.POST, null,
                    new ParameterizedTypeReference<ExternalApiResponse<MachineInfo>>() {});
        } catch (Exception ex) {
            log.warn("[PRODUCTION_RESOURCE] Failed to release machine {}: {}", machineId, ex.getMessage());
        }
    }

    public void releaseEmployee(Long employeeId) {
        try {
            exchange(baseUrl + "/resources/employees/" + employeeId + "/release", HttpMethod.POST, null,
                    new ParameterizedTypeReference<ExternalApiResponse<EmployeeInfo>>() {});
        } catch (Exception ex) {
            log.warn("[PRODUCTION_RESOURCE] Failed to release employee {}: {}", employeeId, ex.getMessage());
        }
    }

    public void releaseTooling(Long toolingId) {
        try {
            exchange(baseUrl + "/resources/tooling/" + toolingId + "/release", HttpMethod.POST, null,
                    new ParameterizedTypeReference<ExternalApiResponse<ToolingInfo>>() {});
        } catch (Exception ex) {
            log.warn("[PRODUCTION_RESOURCE] Failed to release tooling {}: {}", toolingId, ex.getMessage());
        }
    }

    private <T> ExternalApiResponse<T> exchange(String url, HttpMethod method, Object body,
                                                 ParameterizedTypeReference<ExternalApiResponse<T>> type) {
        HttpHeaders headers = new HttpHeaders();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authHeader);
        }
        if (body != null) {
            headers.setContentType(MediaType.APPLICATION_JSON);
        }
        try {
            return restTemplate.exchange(url, method, new HttpEntity<>(body, headers), type).getBody();
        } catch (HttpStatusCodeException ex) {
            throw new IllegalStateException(ex.getResponseBodyAsString());
        }
    }
}
