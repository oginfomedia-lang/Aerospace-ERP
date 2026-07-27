package com.aerospace.qa_service.service;

import com.aerospace.qa_service.dto.FaiCreateRequestDTO;
import com.aerospace.qa_service.dto.FaiPerformRequestDTO;
import com.aerospace.qa_service.dto.FaiResponseDTO;

import java.util.List;

public interface FaiService {
    void autoQueueIfFirstArticle(Long partId, String partName, String skuCode);
    FaiResponseDTO createManual(FaiCreateRequestDTO dto);
    List<FaiResponseDTO> getPendingQueue();
    FaiResponseDTO getById(Long id);
    List<FaiResponseDTO> getByPartId(Long partId);
    FaiResponseDTO performFai(Long id, FaiPerformRequestDTO dto);
}
