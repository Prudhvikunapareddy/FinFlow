export type ApplicationStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'DOCS_VERIFIED'
  | 'APPROVED'
  | 'REJECTED';

export interface ApplicationRequest {
  name: string;
  amount: number;
}

export interface ApplicationResponse {
  id: number;
  name: string;
  applicantName: string;
  amount: number;
  status: ApplicationStatus;
}

export interface AdminApplicationResponse {
  id: number;
  name: string;
  applicantName: string;
  amount: number | null;
  status: ApplicationStatus;
}
