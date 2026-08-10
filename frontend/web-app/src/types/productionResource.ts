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
}

export interface Employee {
  id: number;
  employeeCode: string;
  name: string;
  designation?: string;
  department?: string;
  status: string;
  skills: Skill[];
  createdAt: string;
}

export interface EmployeeRequest {
  name: string;
  designation?: string;
  department?: string;
  status?: string;
  skillIds: number[];
}
