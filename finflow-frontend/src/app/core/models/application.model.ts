export type ApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'DOCS_VERIFIED'
  | 'APPROVED'
  | 'REJECTED';

export type LoanType = 'PERSONAL' | 'HOME' | 'VEHICLE' | 'EDUCATION' | 'BUSINESS';

export const INTEREST_RATES: Record<LoanType, number> = {
  PERSONAL: 10.5,
  HOME: 8.5,
  VEHICLE: 9.0,
  EDUCATION: 8.0,
  BUSINESS: 12.0,
};

export interface ApplicationRequest {
  name: string;
  amount: number;
  loanType?: LoanType;
  tenureMonths?: number;
}

export interface ApplicationResponse {
  id: number;
  name: string;
  applicantName: string;
  amount: number;
  loanType?: LoanType;
  tenureMonths?: number;
  adminNotes?: string;
  status: ApplicationStatus;
  submittedAt?: string;
}

export interface AdminApplicationResponse {
  id: number;
  name: string;
  applicantName: string;
  amount: number | null;
  loanType?: LoanType;
  tenureMonths?: number;
  adminNotes?: string;
  status: ApplicationStatus;
  submittedAt?: string;
}
