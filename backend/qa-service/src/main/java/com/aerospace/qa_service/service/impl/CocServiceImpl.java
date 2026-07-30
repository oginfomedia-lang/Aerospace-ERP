package com.aerospace.qa_service.service.impl;

import com.aerospace.qa_service.dto.CocGenerateRequest;
import com.aerospace.qa_service.dto.CocResponse;
import com.aerospace.qa_service.entity.CertificateOfConformance;
import com.aerospace.qa_service.entity.InspectionReport;
import com.aerospace.qa_service.exception.ResourceNotFoundException;
import com.aerospace.qa_service.repository.CertificateOfConformanceRepository;
import com.aerospace.qa_service.repository.InspectionRepository;
import com.aerospace.qa_service.service.CocService;
import com.aerospace.common.numbering.DocumentSequenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CocServiceImpl implements CocService {

    private final CertificateOfConformanceRepository cocRepository;
    private final InspectionRepository inspectionRepository;
    private final DocumentSequenceService documentSequenceService;

    @Override
    @Transactional
    public CocResponse generate(CocGenerateRequest request) {
        var existing = cocRepository.findByInspectionReportId(request.getInspectionReportId());
        if (existing.isPresent()) {
            return toDTO(existing.get()); // idempotent — same inspection, same certificate
        }

        InspectionReport report = inspectionRepository.findById(request.getInspectionReportId())
                .orElseThrow(() -> new ResourceNotFoundException("Inspection report not found: " + request.getInspectionReportId()));

        if (report.getApprovedQty() == null || report.getApprovedQty() <= 0) {
            throw new IllegalStateException("Cannot certify a report with 0 approved quantity.");
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        CertificateOfConformance coc = CertificateOfConformance.builder()
                .cocNumber(documentSequenceService.generate("COC"))
                .partId(report.getPartId())
                .partName(request.getPartName())
                .skuCode(request.getSkuCode())
                .batchNumber(report.getBatchNumber())
                .poNumber(request.getPoNumber())
                .quantityCertified(report.getApprovedQty())
                .inspectionReport(report)
                .certifyingStandard(request.getCertifyingStandard())
                .issuedBy(request.getIssuedBy())
                .signatureHash(computeSignatureHash(request.getIssuedBy(), issuedAt, report.getId(), request.getCertifyingStandard()))
                .build();

        CertificateOfConformance saved = cocRepository.save(coc);
        log.info("[COC] {} issued for batch {} ({} units)", saved.getCocNumber(), saved.getBatchNumber(), saved.getQuantityCertified());
        return toDTO(saved);
    }

    @Override
    public CocResponse getById(Long id) {
        return toDTO(cocRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found: " + id)));
    }

    @Override
    public List<CocResponse> getByBatch(String batchNumber) {
        return cocRepository.findByBatchNumberOrderByIssuedAtDesc(batchNumber).stream().map(this::toDTO).toList();
    }

    @Override
    public List<CocResponse> getByPart(Long partId) {
        return cocRepository.findByPartIdOrderByIssuedAtDesc(partId).stream().map(this::toDTO).toList();
    }

    private String computeSignatureHash(String issuedBy, LocalDateTime timestamp, Long entityId, String standard) {
        try {
            String payload = issuedBy + "|" + timestamp + "|" + entityId + "|1|" + (standard != null ? standard : "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private CocResponse toDTO(CertificateOfConformance c) {
        return CocResponse.builder()
                .id(c.getId())
                .cocNumber(c.getCocNumber())
                .partId(c.getPartId())
                .partName(c.getPartName())
                .skuCode(c.getSkuCode())
                .batchNumber(c.getBatchNumber())
                .poNumber(c.getPoNumber())
                .quantityCertified(c.getQuantityCertified())
                .inspectionReportId(c.getInspectionReport().getId())
                .certifyingStandard(c.getCertifyingStandard())
                .issuedBy(c.getIssuedBy())
                .issuedAt(c.getIssuedAt())
                .signatureHash(c.getSignatureHash())
                .build();
    }
}
