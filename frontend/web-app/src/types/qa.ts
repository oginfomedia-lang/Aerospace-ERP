// ============================================================
// Matches: EquipmentRequestDTO.java / EquipmentResponseDTO.java (qa-service)
// ============================================================
export interface Equipment {
  id: number;
  assetTag: string;
  name: string;
  equipmentType: 'GAUGE' | 'TOOL' | 'INSTRUMENT' | 'FIXTURE' | 'TEST_EQUIPMENT';
  manufacturer: string | null;
  modelNumber: string | null;
  location: string | null;
  calibrationFrequencyDays: number;
  status: 'ACTIVE' | 'OUT_OF_SERVICE' | 'RETIRED';
  lastCalibratedAt: string | null;
  nextCalibrationDueAt: string | null;
  overdue: boolean;
  createdAt: string;
}

export interface EquipmentRequest {
  assetTag: string;
  name: string;
  equipmentType: string;
  manufacturer?: string;
  modelNumber?: string;
  location?: string;
  calibrationFrequencyDays: number;
}

// ============================================================
// Matches: CalibrationRequestDTO.java / CalibrationRecordResponseDTO.java (qa-service)
// ============================================================
export interface CalibrationRecord {
  id: number;
  equipmentId: number;
  assetTag: string;
  calibratedAt: string;
  performedBy: string | null;
  certificateNumber: string | null;
  result: 'PASS' | 'FAIL' | 'ADJUSTED';
  remarks: string;
  nextDueAt: string;
  createdAt: string;
}

export interface CalibrationRequest {
  calibratedAt?: string;
  performedBy?: string;
  certificateNumber?: string;
  result: string;
  remarks: string;
}

// ============================================================
// Matches: FaiResponseDTO.java / FaiCharacteristicDTO.java (qa-service)
// ============================================================
export interface FaiCharacteristic {
  charNo: number | null;
  description: string | null;
  requirement: string | null;
  measuredResult: string | null;
  result: 'PASS' | 'FAIL' | null;
  remarks: string | null;
}

export interface FaiRecord {
  id: number;
  faiNumber: string;
  partId: number;
  partName: string | null;
  skuCode: string | null;
  partRevision: string | null;
  batchNumber: string | null;
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETE';
  overallResult: 'CONFORMING' | 'NON_CONFORMING' | null;
  materialCertRef: string | null;
  inspectorName: string | null;
  performedAt: string | null;
  remarks: string | null;
  signatureHash: string | null;
  createdAt: string;
  characteristics: FaiCharacteristic[];
}

// ============================================================
// Matches: CocResponseDTO.java (qa-service)
// ============================================================
export interface CertificateOfConformance {
  id: number;
  cocNumber: string;
  partId: number;
  partName: string | null;
  skuCode: string | null;
  batchNumber: string;
  poNumber: string | null;
  quantityCertified: number;
  inspectionReportId: number;
  certifyingStandard: string | null;
  issuedBy: string;
  issuedAt: string;
  signatureHash: string;
}

