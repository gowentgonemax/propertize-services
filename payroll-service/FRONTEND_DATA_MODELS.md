# WageCraft Frontend Data Models & TypeScript Interfaces

This document provides TypeScript interfaces and data models for the WageCraft application.

---

## Core Models

### User
```typescript
interface User {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  createdAt: string;
  updatedAt: string;
}
```

### Address
```typescript
interface Address {
  street: string;
  city: string;
  state: string;
  zipCode: string;
  country: string; // Default: "USA"
}
```

---

## Client Management

### Client
```typescript
interface Client {
  id: string;
  companyName: string;
  taxId: string;
  address: Address;
  contactPerson: string;
  email: string;
  phone: string;
  status: ClientStatus;
  payrollSchedule: PayrollSchedule;
  createdAt: string;
  updatedAt: string;
}

type ClientStatus = 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';

type PayrollSchedule = 
  | 'WEEKLY'
  | 'BIWEEKLY'
  | 'SEMI_MONTHLY'
  | 'MONTHLY';
```

---

## Employee Management

### Employee
```typescript
interface Employee {
  id: string;
  client?: Client; // May be populated or just ID
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: Address;
  ssn: string; // Masked in responses: "***-**-1234"
  dateOfBirth: string; // ISO date: "YYYY-MM-DD"
  hireDate: string; // ISO date: "YYYY-MM-DD"
  terminationDate?: string | null; // ISO date: "YYYY-MM-DD"
  department: string;
  position: string;
  employmentType: EmploymentType;
  payType: PayType;
  payRate: number;
  status: EmployeeStatus;
  createdAt: string;
  updatedAt: string;
}

type EmploymentType = 
  | 'FULL_TIME'
  | 'PART_TIME'
  | 'CONTRACT'
  | 'TEMPORARY'
  | 'SEASONAL';

type PayType = 
  | 'SALARY'
  | 'HOURLY'
  | 'COMMISSION'
  | 'CONTRACTOR';

type EmployeeStatus = 
  | 'ACTIVE'
  | 'INACTIVE'
  | 'TERMINATED'
  | 'ON_LEAVE'
  | 'SUSPENDED';
```

---

## Payroll Management

### PayrollRun
```typescript
interface PayrollRun {
  id: string;
  client?: Client;
  payPeriodStart: string; // ISO date: "YYYY-MM-DD"
  payPeriodEnd: string; // ISO date: "YYYY-MM-DD"
  payDate: string; // ISO date: "YYYY-MM-DD"
  status: PayrollStatus;
  totalGrossPay?: number;
  totalNetPay?: number;
  totalTaxes?: number;
  totalDeductions?: number;
  employeeCount?: number;
  processedAt?: string | null; // ISO datetime
  approvedAt?: string | null; // ISO datetime
  approvedBy?: User | null;
  createdAt: string;
  updatedAt: string;
}

type PayrollStatus = 
  | 'DRAFT'
  | 'PROCESSING'
  | 'PROCESSED'
  | 'APPROVED'
  | 'PAID'
  | 'CANCELLED';
```

### Paystub
```typescript
interface Paystub {
  id: string;
  payrollRun: PayrollRun;
  employee: Employee;
  payPeriodStart: string;
  payPeriodEnd: string;
  payDate: string;
  grossPay: number;
  netPay: number;
  totalTaxes: number;
  totalDeductions: number;
  regularHours?: number;
  overtimeHours?: number;
  regularPay?: number;
  overtimePay?: number;
  earnings: PaystubEarning[];
  deductions: PaystubDeduction[];
  taxes: PaystubTax[];
  createdAt: string;
  updatedAt: string;
}
```

### PaystubEarning
```typescript
interface PaystubEarning {
  id: string;
  earningType: EarningType;
  description: string;
  amount: number;
  hours?: number;
  rate?: number;
}

type EarningType = 
  | 'REGULAR'
  | 'OVERTIME'
  | 'BONUS'
  | 'COMMISSION'
  | 'HOLIDAY'
  | 'SICK'
  | 'VACATION'
  | 'PTO'
  | 'SHIFT_DIFFERENTIAL'
  | 'SEVERANCE'
  | 'OTHER';
```

### PaystubDeduction
```typescript
interface PaystubDeduction {
  id: string;
  deductionType: DeductionType;
  description: string;
  amount: number;
}

type DeductionType = 
  | 'HEALTH_INSURANCE'
  | 'DENTAL_INSURANCE'
  | 'VISION_INSURANCE'
  | 'LIFE_INSURANCE'
  | 'RETIREMENT_401K'
  | 'RETIREMENT_403B'
  | 'HSA'
  | 'FSA'
  | 'GARNISHMENT'
  | 'CHILD_SUPPORT'
  | 'UNION_DUES'
  | 'OTHER';
```

### PaystubTax
```typescript
interface PaystubTax {
  id: string;
  taxType: TaxType;
  description: string;
  amount: number;
  taxableWages?: number;
}

type TaxType = 
  | 'FEDERAL_INCOME'
  | 'STATE_INCOME'
  | 'LOCAL_INCOME'
  | 'SOCIAL_SECURITY'
  | 'MEDICARE'
  | 'MEDICARE_ADDITIONAL'
  | 'STATE_DISABILITY'
  | 'STATE_UNEMPLOYMENT'
  | 'FEDERAL_UNEMPLOYMENT';
```

---

## Benefits & Compensation

### BenefitPlan
```typescript
interface BenefitPlan {
  id: string;
  client: Client;
  planName: string;
  benefitType: BenefitType;
  provider?: string;
  description?: string;
  employerCost: number;
  employeeCost: number;
  coverageLevel: CoverageLevel;
  effectiveDate: string;
  endDate?: string | null;
  status: PlanStatus;
  createdAt: string;
  updatedAt: string;
}

type BenefitType = 
  | 'HEALTH'
  | 'DENTAL'
  | 'VISION'
  | 'LIFE'
  | 'DISABILITY_SHORT_TERM'
  | 'DISABILITY_LONG_TERM'
  | 'RETIREMENT_401K'
  | 'RETIREMENT_403B'
  | 'HSA'
  | 'FSA'
  | 'COMMUTER'
  | 'OTHER';

type CoverageLevel = 
  | 'EMPLOYEE_ONLY'
  | 'EMPLOYEE_SPOUSE'
  | 'EMPLOYEE_CHILDREN'
  | 'FAMILY';

type PlanStatus = 
  | 'ACTIVE'
  | 'INACTIVE'
  | 'EXPIRED';
```

### BenefitEnrollment
```typescript
interface BenefitEnrollment {
  id: string;
  employee: Employee;
  benefitPlan: BenefitPlan;
  enrollmentDate: string;
  effectiveDate: string;
  endDate?: string | null;
  status: EnrollmentStatus;
  employeeContribution: number;
  employerContribution: number;
  createdAt: string;
  updatedAt: string;
}

type EnrollmentStatus = 
  | 'PENDING'
  | 'ACTIVE'
  | 'CANCELLED'
  | 'EXPIRED';
```

### Compensation
```typescript
interface Compensation {
  id: string;
  employee: Employee;
  compensationType: CompensationType;
  amount: number;
  effectiveDate: string;
  endDate?: string | null;
  status: CompensationStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

type CompensationType = 
  | 'BASE_SALARY'
  | 'HOURLY_RATE'
  | 'BONUS'
  | 'COMMISSION'
  | 'STOCK_OPTIONS'
  | 'ALLOWANCE'
  | 'OTHER';

type CompensationStatus = 
  | 'ACTIVE'
  | 'INACTIVE'
  | 'PENDING'
  | 'EXPIRED';
```

---

## Time & Attendance

### TimeEntry
```typescript
interface TimeEntry {
  id: string;
  employee: Employee;
  date: string; // ISO date
  clockIn: string; // ISO datetime
  clockOut?: string | null; // ISO datetime
  regularHours: number;
  overtimeHours: number;
  breakMinutes?: number;
  status: TimeEntryStatus;
  notes?: string;
  createdAt: string;
  updatedAt: string;
}

type TimeEntryStatus = 
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'SUBMITTED';
```

### LeaveRequest
```typescript
interface LeaveRequest {
  id: string;
  employee: Employee;
  leaveType: LeaveType;
  startDate: string; // ISO date
  endDate: string; // ISO date
  totalDays: number;
  reason?: string;
  status: LeaveStatus;
  approvedBy?: User | null;
  approvedAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

type LeaveType = 
  | 'VACATION'
  | 'SICK'
  | 'PERSONAL'
  | 'BEREAVEMENT'
  | 'JURY_DUTY'
  | 'MILITARY'
  | 'UNPAID'
  | 'FMLA'
  | 'PARENTAL'
  | 'SABBATICAL';

type LeaveStatus = 
  | 'PENDING'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED';
```

---

## Authentication

### LoginRequest
```typescript
interface LoginRequest {
  email: string;
  password: string;
}
```

### LoginResponse
```typescript
interface LoginResponse {
  token: string;
  refreshToken: string;
  expiresIn: number; // seconds
}
```

### RegisterRequest
```typescript
interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
}
```

### RegisterResponse
```typescript
interface RegisterResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  message: string;
}
```

---

## Pagination

### PaginatedResponse<T>
```typescript
interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  first: boolean;
}

// Usage examples:
// PaginatedResponse<Client>
// PaginatedResponse<Employee>
// PaginatedResponse<PayrollRun>
```

---

## API Error Response

### ErrorResponse
```typescript
interface ErrorResponse {
  error: string;
  message: string;
  timestamp?: string;
  path?: string;
}
```

---

## Form Data Types

### ClientFormData
```typescript
interface ClientFormData {
  companyName: string;
  taxId: string;
  address: Address;
  contactPerson: string;
  email: string;
  phone: string;
  status: ClientStatus;
  payrollSchedule: PayrollSchedule;
}
```

### EmployeeFormData
```typescript
interface EmployeeFormData {
  employeeNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  phone: string;
  address: Address;
  ssn: string;
  dateOfBirth: string;
  hireDate: string;
  department: string;
  position: string;
  employmentType: EmploymentType;
  payType: PayType;
  payRate: number;
  status: EmployeeStatus;
}
```

### PayrollRunFormData
```typescript
interface PayrollRunFormData {
  payPeriodStart: string;
  payPeriodEnd: string;
  payDate: string;
  status: PayrollStatus;
}
```

---

## Utility Types

### SelectOption
```typescript
interface SelectOption<T = string> {
  value: T;
  label: string;
}

// Example usage for dropdowns:
const clientStatusOptions: SelectOption<ClientStatus>[] = [
  { value: 'ACTIVE', label: 'Active' },
  { value: 'INACTIVE', label: 'Inactive' },
  { value: 'SUSPENDED', label: 'Suspended' },
];
```

### FilterParams
```typescript
interface FilterParams {
  page?: number;
  limit?: number;
  sortBy?: string;
  sortOrder?: 'asc' | 'desc';
  [key: string]: any; // Additional filter fields
}
```

