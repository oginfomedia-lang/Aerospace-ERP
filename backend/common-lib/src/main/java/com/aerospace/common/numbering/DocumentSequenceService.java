package com.aerospace.common.numbering;

import java.util.List;

public interface DocumentSequenceService {
    String generate(String docType); // uses default company/plant
    String generate(String docType, String companyCode, String plantCode);
    List<DocumentSequence> getAll();
    DocumentSequence updateConfig(String docType, String prefix, String suffix, Integer padding);
}