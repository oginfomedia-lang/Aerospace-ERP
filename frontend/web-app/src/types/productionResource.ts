// Matches: MachineRequestDTO.java / MachineResponseDTO.java (production-resource-service)
export interface Machine {
  id: number;
  machineCode: string;
  name: string;
  machineType: string;
  manufacturer?: string;
  modelNumber?: string;
  location?: string;
  capacity?: string;
  maintenanceFrequencyDays: number;
  status: string;
  linkedEquipmentId?: number | null;
  lastMaintenanceAt?: string | null;
  nextMaintenanceDueAt?: string | null;
  overdue: boolean;
  occupancy: string;
  currentStageLogId?: number | null;
  version?: number | null;
  createdAt: string;
}

export interface MachineRequest {
  machineCode: string;
  name: string;
  machineType: string;
  manufacturer?: string;
  modelNumber?: string;
  location?: string;
  capacity?: string;
  maintenanceFrequencyDays: number;
}

// Matches: MaintenanceRequestDTO.java / MaintenanceRecordResponseDTO.java
export interface MaintenanceRecord {
  id: number;
  machineId: number;
  machineCode: string;
  maintenanceType: string;
  performedAt: string;
  performedBy?: string;
  cost?: number;
  downtimeMinutes?: number;
  result: string;
  remarks: string;
  nextDueAt: string;
  createdAt: string;
}

export interface MaintenanceRequest {
  maintenanceType: string;
  performedBy: string;
  cost?: number;
  downtimeMinutes?: number;
  result: string;
  remarks: string;
}

// Matches: SkillResponseDTO.java / EmployeeResponseDTO.java (production-resource-service)
export interface Skill {
  id: number;
  name: string;
  description?: string | null;
  certificationRequired: boolean;
}

// The CURRENT certified state of one skill for an employee — matches
// EmployeeSkillSummaryDTO.java, derived server-side from certification history.
export interface EmployeeSkillSummary {
  skillId: number;
  skillName: string;
  level: number;
  expiryDate?: string | null;
  status: string; // ACTIVE, EXPIRED, REVOKED
}

export interface Employee {
  id: number;
  employeeCode: string;
  name: string;
  designation?: string;
  department?: string;
  status: string;
  skills: EmployeeSkillSummary[];
  shiftId?: number | null;
  shiftName?: string | null;
  occupancy: string;
  currentStageLogId?: number | null;
  version?: number | null;
  createdAt: string;
}

export interface EmployeeRequest {
  name: string;
  designation?: string;
  department?: string;
  status?: string;
  shiftId?: number | null;
}

// Matches: ShiftRequestDTO.java / ShiftResponseDTO.java — startTime/endTime are
// "HH:mm:ss" strings over the wire (java.time.LocalTime's default JSON shape).
export interface Shift {
  id: number;
  name: string;
  startTime: string;
  endTime: string;
  daysOfWeek: string; // e.g. "MON,TUE,WED,THU,FRI"
}

export interface ShiftRequest {
  name: string;
  startTime: string;
  endTime: string;
  daysOfWeek: string;
}

// Matches: EmployeeLeaveRequestDTO.java / EmployeeLeaveResponseDTO.java — one leave
// request per row, moving PENDING -> APPROVED/REJECTED in place.
export interface EmployeeLeaveRequest {
  type: string; // SICK, VACATION, OTHER
  startDate: string;
  endDate: string;
  reason?: string;
}

export interface EmployeeLeave {
  id: number;
  employeeId: number;
  employeeCode: string;
  type: string;
  startDate: string;
  endDate: string;
  reason?: string | null;
  status: string; // PENDING, APPROVED, REJECTED
  decidedBy?: string | null;
  decidedAt?: string | null;
  createdAt: string;
}

// Matches: EmployeeSkillCertificationRequestDTO.java / ...ResponseDTO.java — an
// append-only certification/evaluation event, recorded via POST /employees/{id}/skills.
export interface EmployeeSkillCertificationRequest {
  skillId: number;
  level: number; // 1-5: Beginner..Expert by convention
  experienceHours?: number;
  certificateNumber?: string;
  issuedDate?: string;
  expiryDate?: string;
  evaluatedBy?: string;
  evaluationDate?: string;
  nextEvaluationDate?: string;
  remarks?: string;
  // Omit for the normal case (defaults to ACTIVE server-side). Set to "REVOKED" to
  // explicitly record that a certification is no longer valid — a new row, not an
  // edit of the old one, same append-only pattern as every other field here.
  status?: string;
}

export interface EmployeeSkillCertification {
  id: number;
  employeeId: number;
  skillId: number;
  skillName: string;
  level: number;
  experienceHours?: number | null;
  certificateNumber?: string | null;
  issuedDate?: string | null;
  expiryDate?: string | null;
  evaluatedBy?: string | null;
  evaluationDate?: string | null;
  nextEvaluationDate?: string | null;
  status: string;
  remarks?: string | null;
  createdAt: string;
}

// Matches: SkillUpgradeRequestCreateDTO.java / SkillUpgradeResponseDTO.java — a
// PENDING -> APPROVED/REJECTED request gating a skill-level INCREASE. Approval writes
// a real EmployeeSkillCertification row server-side; this only tracks the decision.
export interface SkillUpgradeRequestCreate {
  skillId: number;
  toLevel: number;
  reason?: string;
  requestedBy?: string;
}

export interface SkillUpgradeDecision {
  decidedBy: string;
}

export interface SkillUpgradeRequest {
  id: number;
  employeeId: number;
  employeeName: string;
  skillId: number;
  skillName: string;
  fromLevel: number;
  toLevel: number;
  status: string; // PENDING, APPROVED, REJECTED
  reason?: string | null;
  requestedBy?: string | null;
  createdAt: string;
  decidedBy?: string | null;
  decidedAt?: string | null;
}

// Matches: ToolingRequestDTO.java / ToolingResponseDTO.java (production-resource-service)
export interface Tooling {
  id: number;
  toolCode: string;
  name: string;
  toolingType: string; // CUTTING_TOOL, FIXTURE, JIG, MEASURING_EQUIPMENT, OTHER
  location?: string | null;
  status: string; // ACTIVE, WORN, RETIRED
  occupancy: string; // FREE, IN_OPERATION
  currentStageLogId?: number | null;
  version?: number | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface ToolingRequest {
  name: string;
  toolingType: string;
  location?: string;
  status?: string;
}

// Matches: MachineReservationRequestDTO.java / MachineReservationResponseDTO.java —
// an ADVANCE BOOKING for a future time window, distinct from Machine.occupancy
// (which only ever reflects "busy right now"). reservedFrom/reservedTo are ISO
// datetime strings ("2026-08-14T10:00:00").
export interface MachineReservation {
  id: number;
  machineId: number;
  machineCode: string;
  reservedFrom: string;
  reservedTo: string;
  productionOrderId?: number | null;
  createdBy: string;
  status: string; // ACTIVE, CANCELLED
  version?: number | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface MachineReservationRequest {
  machineId: number;
  reservedFrom: string;
  reservedTo: string;
  productionOrderId?: number | null;
  createdBy: string;
}

export interface EmployeeReservation {
  id: number;
  employeeId: number;
  employeeCode: string;
  reservedFrom: string;
  reservedTo: string;
  productionOrderId?: number | null;
  createdBy: string;
  status: string;
  version?: number | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface EmployeeReservationRequest {
  employeeId: number;
  reservedFrom: string;
  reservedTo: string;
  productionOrderId?: number | null;
  createdBy: string;
  requiredSkillId?: number | null;
  minimumSkillLevel?: number | null;
}
