package com.aerospace.qa_service.service;

import com.aerospace.qa_service.dto.CocGenerateRequest;
import com.aerospace.qa_service.dto.CocResponse;

import java.util.List;

public interface CocService {
    CocResponse generate(CocGenerateRequest request);
    CocResponse getById(Long id);
    List<CocResponse> getByBatch(String batchNumber);
    List<CocResponse> getByPart(Long partId);
}
